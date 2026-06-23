package com.czspig.productcritic.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.czspig.productcritic.ai.AiProvider;
import com.czspig.productcritic.ai.ProductReviewOutputParser;
import com.czspig.productcritic.ai.ProductReviewPromptBuilder;
import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.common.PageResponse;
import com.czspig.productcritic.common.ReviewMode;
import com.czspig.productcritic.common.ReviewStatus;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewDetailResponse;
import com.czspig.productcritic.dto.ReviewListItemResponse;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.czspig.productcritic.entity.AiCallLogEntity;
import com.czspig.productcritic.entity.ReviewRecordEntity;
import com.czspig.productcritic.mapper.AiCallLogMapper;
import com.czspig.productcritic.mapper.ReviewRecordMapper;
import com.czspig.productcritic.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROMPT_VERSION = "mvp-v1";

    private final ReviewRecordMapper reviewRecordMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiProvider aiProvider;
    private final ProductReviewPromptBuilder promptBuilder;
    private final ProductReviewOutputParser outputParser;
    private final ObjectMapper objectMapper;

    public ReviewServiceImpl(
            ReviewRecordMapper reviewRecordMapper,
            AiCallLogMapper aiCallLogMapper,
            AiProvider aiProvider,
            ProductReviewPromptBuilder promptBuilder,
            ProductReviewOutputParser outputParser,
            ObjectMapper objectMapper) {
        this.reviewRecordMapper = reviewRecordMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.aiProvider = aiProvider;
        this.promptBuilder = promptBuilder;
        this.outputParser = outputParser;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(noRollbackFor = BizException.class)
    public ReviewDetailResponse createReview(CreateReviewRequest request, String sessionId) {
        ReviewMode mode = ReviewMode.requireValid(request.getMode());
        String safeContent = normalizeContent(request.getContent());
        String safeSessionId = normalizeSessionId(sessionId);

        CreateReviewRequest normalizedRequest = new CreateReviewRequest();
        normalizedRequest.setContent(safeContent);
        normalizedRequest.setMode(mode.name());
        normalizedRequest.setRoastLevel(request.getRoastLevel());

        String prompt = promptBuilder.build(normalizedRequest);
        long startedAt = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        ReviewRecordEntity record = new ReviewRecordEntity();
        record.setSessionId(safeSessionId);
        record.setInputContent(safeContent);
        record.setInputSummary(buildSummary(safeContent));
        record.setMode(mode.name());
        record.setRoastLevel(request.getRoastLevel());
        record.setOneLineVerdict("AI 评审生成中");
        record.setBeatScore(0);
        record.setPositioningScore(0);
        record.setReportJson("{}");
        record.setReportMarkdown("");
        record.setStatus(ReviewStatus.PENDING.name());
        record.setModelName("pending");
        record.setPromptVersion(PROMPT_VERSION);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        reviewRecordMapper.insert(record);

        try {
            ReviewReportDto report = aiProvider.review(normalizedRequest, prompt);
            outputParser.validate(report);

            String reportJson = writeJson(report);
            String reportMarkdown = renderMarkdown(safeContent, mode, request.getRoastLevel(), report);
            LocalDateTime finishedAt = LocalDateTime.now();
            record.setOneLineVerdict(report.getOneLineVerdict());
            record.setBeatScore(report.getBeatScore());
            record.setPositioningScore(report.getPositioningScore());
            record.setReportJson(reportJson);
            record.setReportMarkdown(reportMarkdown);
            record.setStatus(ReviewStatus.SUCCESS.name());
            record.setErrorMessage(null);
            record.setModelName(safeModelName());
            record.setUpdatedAt(finishedAt);
            reviewRecordMapper.updateById(record);

            insertAiCallLog(record, prompt, report, startedAt);
            return toDetailResponse(record, report);
        } catch (BizException ex) {
            markFailed(record, prompt, startedAt, ex);
            throw ex;
        } catch (RuntimeException ex) {
            BizException wrapped = new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 评审服务暂时不可用，请稍后再试");
            markFailed(record, prompt, startedAt, wrapped);
            throw wrapped;
        }
    }

    @Override
    public PageResponse<ReviewListItemResponse> listReviews(int page, int pageSize, String sessionId) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        String safeSessionId = normalizeSessionId(sessionId);

        Page<ReviewRecordEntity> queryPage = new Page<>(safePage, safePageSize);
        LambdaQueryWrapper<ReviewRecordEntity> wrapper = new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getSessionId, safeSessionId)
                .isNull(ReviewRecordEntity::getDeletedAt)
                .orderByDesc(ReviewRecordEntity::getCreatedAt);
        Page<ReviewRecordEntity> result = reviewRecordMapper.selectPage(queryPage, wrapper);
        List<ReviewListItemResponse> items = result.getRecords()
                .stream()
                .map(this::toListItemResponse)
                .toList();
        return new PageResponse<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    @Override
    public ReviewDetailResponse getReview(Long id, String sessionId) {
        if (id == null || id <= 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "id 必须为正整数");
        }
        String safeSessionId = normalizeSessionId(sessionId);
        ReviewRecordEntity record = reviewRecordMapper.selectById(id);
        if (record == null || record.getDeletedAt() != null || !safeSessionId.equals(record.getSessionId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "评审记录不存在");
        }
        return toDetailResponse(record, readReport(record.getReportJson()));
    }

    private void insertAiCallLog(
            ReviewRecordEntity record,
            String prompt,
            ReviewReportDto report,
            long startedAt) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setReviewRecordId(record.getId());
        log.setProvider(aiProvider.providerName());
        log.setModelName(aiProvider.modelName());
        log.setPromptHash(sha256(prompt));
        log.setRequestSummary("mode=%s, roastLevel=%d, contentLength=%d".formatted(
                record.getMode(), record.getRoastLevel(), record.getInputContent().length()));
        log.setResponseSummary(report.getOneLineVerdict());
        log.setInputTokens(0);
        log.setOutputTokens(0);
        log.setLatencyMs((int) Math.max(0, System.currentTimeMillis() - startedAt));
        log.setStatus(ReviewStatus.SUCCESS.name());
        log.setCreatedAt(LocalDateTime.now());
        aiCallLogMapper.insert(log);
    }

    private ReviewDetailResponse toDetailResponse(ReviewRecordEntity record, ReviewReportDto report) {
        ReviewDetailResponse response = new ReviewDetailResponse();
        response.setId(record.getId());
        response.setInputContent(record.getInputContent());
        response.setInputSummary(record.getInputSummary());
        response.setMode(record.getMode());
        response.setRoastLevel(record.getRoastLevel());
        response.setOneLineVerdict(record.getOneLineVerdict());
        response.setBeatScore(record.getBeatScore());
        response.setPositioningScore(record.getPositioningScore());
        response.setReport(report);
        response.setReportJson(record.getReportJson());
        response.setReportMarkdown(record.getReportMarkdown());
        response.setStatus(record.getStatus());
        response.setErrorMessage(record.getErrorMessage());
        response.setCreatedAt(formatTime(record.getCreatedAt()));
        return response;
    }

    private ReviewListItemResponse toListItemResponse(ReviewRecordEntity record) {
        ReviewListItemResponse response = new ReviewListItemResponse();
        response.setId(record.getId());
        response.setInputSummary(record.getInputSummary());
        response.setMode(record.getMode());
        response.setRoastLevel(record.getRoastLevel());
        response.setOneLineVerdict(record.getOneLineVerdict());
        response.setBeatScore(record.getBeatScore());
        response.setPositioningScore(record.getPositioningScore());
        response.setStatus(record.getStatus());
        response.setErrorMessage(record.getErrorMessage());
        response.setCreatedAt(formatTime(record.getCreatedAt()));
        return response;
    }

    private void markFailed(
            ReviewRecordEntity record,
            String prompt,
            long startedAt,
            BizException ex) {
        String safeMessage = sanitizeError(ex.getMessage());
        LocalDateTime failedAt = LocalDateTime.now();
        record.setStatus(ReviewStatus.FAILED.name());
        record.setErrorMessage(safeMessage);
        record.setModelName(safeModelName());
        record.setUpdatedAt(failedAt);
        reviewRecordMapper.updateById(record);

        AiCallLogEntity log = new AiCallLogEntity();
        log.setReviewRecordId(record.getId());
        log.setProvider(safeProviderName());
        log.setModelName(record.getModelName());
        log.setPromptHash(sha256(prompt));
        log.setRequestSummary("mode=%s, roastLevel=%d, contentLength=%d".formatted(
                record.getMode(), record.getRoastLevel(), record.getInputContent().length()));
        log.setResponseSummary(null);
        log.setInputTokens(0);
        log.setOutputTokens(0);
        log.setLatencyMs((int) Math.max(0, System.currentTimeMillis() - startedAt));
        log.setStatus(ReviewStatus.FAILED.name());
        log.setErrorCode(ex.getErrorCode().getCode());
        log.setErrorMessage(safeMessage);
        log.setCreatedAt(failedAt);
        aiCallLogMapper.insert(log);
    }

    private String sanitizeError(String message) {
        String value = message == null || message.isBlank() ? "AI 评审失败" : message.trim();
        value = value.replaceAll("(?i)sk-[a-z0-9_-]+", "sk-***");
        return value.length() <= 200 ? value : value.substring(0, 200);
    }

    private String safeProviderName() {
        try {
            return aiProvider.providerName();
        } catch (RuntimeException ex) {
            return "unknown";
        }
    }

    private String safeModelName() {
        try {
            return aiProvider.modelName();
        } catch (RuntimeException ex) {
            return "unknown";
        }
    }

    private String normalizeContent(String content) {
        String value = content == null ? "" : content.trim();
        if (value.length() < 10 || value.length() > 5000) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "产品想法长度应为 10-5000 个字符");
        }
        return value;
    }

    private String normalizeSessionId(String sessionId) {
        String value = sessionId == null || sessionId.isBlank() ? "anonymous-dev-session" : sessionId.trim();
        if (value.length() > 128) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "X-Session-Id 不能超过 128 个字符");
        }
        return value;
    }

    private String buildSummary(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
    }

    private String writeJson(ReviewReportDto report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "评审报告序列化失败");
        }
    }

    private ReviewReportDto readReport(String reportJson) {
        try {
            return objectMapper.readValue(reportJson, ReviewReportDto.class);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "评审报告解析失败");
        }
    }

    private String renderMarkdown(
            String content,
            ReviewMode mode,
            Integer roastLevel,
            ReviewReportDto report) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 猪猪产品毒舌官评审报告\n\n");
        builder.append("## 1. 一句话评价\n\n").append(report.getOneLineVerdict()).append("\n\n");
        builder.append("## 2. 毒打指数\n\n").append(report.getBeatScore()).append("/100\n\n");
        builder.append("## 3. 产品定位评分\n\n").append(report.getPositioningScore()).append("/100\n\n");
        builder.append("## 4. 用户痛点分析\n\n").append(report.getPainPointAnalysis()).append("\n\n");
        appendList(builder, "## 5. 伪需求风险", report.getFakeDemandRisks());
        appendList(builder, "## 6. 功能冗余检查", report.getFeatureRedundancyCheck());
        appendList(builder, "## 7. 冷启动问题", report.getColdStartProblems());
        appendList(builder, "## 8. MVP 改造建议", report.getMvpSuggestions());
        builder.append("## 9. 最小可开发版本\n\n");
        builder.append("目标：").append(report.getMinimumBuildVersion().getGoal()).append("\n\n");
        appendList(builder, "核心功能", report.getMinimumBuildVersion().getCoreFeatures());
        appendList(builder, "暂不实现", report.getMinimumBuildVersion().getExcludedFeatures());
        builder.append("## 10. 给 Codex/Cursor 的开发 Prompt\n\n");
        builder.append("```markdown\n").append(report.getDeveloperPrompt()).append("\n```\n\n");
        builder.append("---\n\n");
        builder.append("评审模式：").append(mode.name()).append("\n\n");
        builder.append("吐槽强度：").append(roastLevel).append("\n\n");
        builder.append("原始输入摘要：").append(buildSummary(content)).append("\n");
        return builder.toString();
    }

    private void appendList(StringBuilder builder, String title, List<String> values) {
        builder.append(title).append("\n\n");
        for (String value : values) {
            builder.append("- ").append(value).append("\n");
        }
        builder.append("\n");
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : TIME_FORMATTER.format(time);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            return "sha256-unavailable";
        }
    }
}
