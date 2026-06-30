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
import com.czspig.productcritic.dto.ReviewGroupResponse;
import com.czspig.productcritic.dto.ReviewListItemResponse;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.czspig.productcritic.entity.AiCallLogEntity;
import com.czspig.productcritic.entity.ReviewRecordEntity;
import com.czspig.productcritic.mapper.AiCallLogMapper;
import com.czspig.productcritic.mapper.ReviewRecordMapper;
import com.czspig.productcritic.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROMPT_VERSION = "p2-review-quality-v1";

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
        normalizedRequest.setIdeaGroupId(request.getIdeaGroupId());
        normalizedRequest.setParentReviewId(request.getParentReviewId());

        String prompt = promptBuilder.build(normalizedRequest);
        String promptHash = sha256(prompt);
        log.debug(
                "create review: contentPreview={}, providerBeforeCall={}, promptHash={}",
                preview(safeContent, 80),
                safeProviderName(),
                promptHash
        );
        long startedAt = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        ReviewRecordEntity record = new ReviewRecordEntity();
        record.setSessionId(safeSessionId);
        record.setInputContent(safeContent);
        record.setInputSummary(buildSummary(safeContent));
        applyVersionFields(record, request, safeSessionId);
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
        ensureRecordGroupId(record);

        try {
            ReviewReportDto report = aiProvider.review(normalizedRequest, prompt);
            AiProvider.ProviderExecution execution = aiProvider.execution();
            log.debug(
                    "review success: provider={}, model={}, fallbackUsed={}, promptHash={}",
                    execution.providerName(),
                    execution.modelName(),
                    execution.fallbackUsed(),
                    promptHash
            );
            outputParser.validate(report);

            String reportJson = writeJson(report);
            String reportMarkdown = renderMarkdown(safeContent, mode, request.getRoastLevel(), report, execution);
            LocalDateTime finishedAt = LocalDateTime.now();
            record.setOneLineVerdict(report.getOneLineVerdict());
            record.setBeatScore(report.getBeatScore());
            record.setPositioningScore(report.getPositioningScore());
            record.setReportJson(reportJson);
            record.setReportMarkdown(reportMarkdown);
            record.setStatus(ReviewStatus.SUCCESS.name());
            record.setErrorMessage(null);
            record.setModelName(execution.modelName());
            record.setUpdatedAt(finishedAt);
            reviewRecordMapper.updateById(record);

            insertAiCallLog(record, promptHash, report, startedAt, execution);
            return toDetailResponse(record, report, execution);
        } catch (BizException ex) {
            markFailed(record, promptHash, startedAt, ex);
            throw ex;
        } catch (RuntimeException ex) {
            BizException wrapped = new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 评审服务暂时不可用，请稍后再试");
            markFailed(record, promptHash, startedAt, wrapped);
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

    @Override
    public ReviewGroupResponse getReviewGroup(String ideaGroupId, String sessionId) {
        String safeSessionId = normalizeSessionId(sessionId);
        String safeGroupId = normalizeIdeaGroupId(ideaGroupId);
        List<ReviewRecordEntity> records = reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getSessionId, safeSessionId)
                .eq(ReviewRecordEntity::getIdeaGroupId, safeGroupId)
                .isNull(ReviewRecordEntity::getDeletedAt)
                .orderByAsc(ReviewRecordEntity::getVersionNo)
                .orderByAsc(ReviewRecordEntity::getCreatedAt));
        if (records.isEmpty()) {
            ReviewRecordEntity legacyRecord = findLegacyGroupRecord(safeGroupId, safeSessionId);
            if (legacyRecord != null) {
                records = List.of(legacyRecord);
            }
        }
        if (records.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "迭代版本不存在");
        }

        ReviewGroupResponse response = new ReviewGroupResponse();
        response.setIdeaGroupId(safeGroupId);
        response.setVersions(records.stream().map(this::toReviewVersionItem).toList());
        return response;
    }

    private void applyVersionFields(ReviewRecordEntity record, CreateReviewRequest request, String sessionId) {
        ReviewRecordEntity parent = findParentRecord(request.getParentReviewId(), sessionId);
        if (parent != null) {
            String parentGroupId = normalizeRecordGroupId(parent);
            record.setIdeaGroupId(parentGroupId);
            record.setParentReviewId(parent.getId());
            record.setVersionNo(nextVersionNo(parentGroupId, sessionId, parent));
            return;
        }

        String requestedGroupId = blankToNull(request.getIdeaGroupId());
        if (requestedGroupId != null) {
            String safeGroupId = normalizeIdeaGroupId(requestedGroupId);
            record.setIdeaGroupId(safeGroupId);
            record.setParentReviewId(null);
            record.setVersionNo(nextVersionNo(safeGroupId, sessionId, null));
            return;
        }

        record.setIdeaGroupId(null);
        record.setParentReviewId(null);
        record.setVersionNo(1);
    }

    private void insertAiCallLog(
            ReviewRecordEntity record,
            String promptHash,
            ReviewReportDto report,
            long startedAt,
            AiProvider.ProviderExecution execution) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setReviewRecordId(record.getId());
        log.setProvider(execution.providerName());
        log.setModelName(execution.modelName());
        log.setPromptHash(promptHash);
        log.setRequestSummary("mode=%s, roastLevel=%d, contentLength=%d, contentPreview=%s, fallbackUsed=%s".formatted(
                record.getMode(),
                record.getRoastLevel(),
                record.getInputContent().length(),
                preview(record.getInputContent(), 80),
                execution.fallbackUsed()));
        log.setResponseSummary(report.getOneLineVerdict());
        log.setInputTokens(0);
        log.setOutputTokens(0);
        log.setLatencyMs((int) Math.max(0, System.currentTimeMillis() - startedAt));
        log.setStatus(ReviewStatus.SUCCESS.name());
        log.setCreatedAt(LocalDateTime.now());
        aiCallLogMapper.insert(log);
    }

    private ReviewRecordEntity findParentRecord(Long parentReviewId, String sessionId) {
        if (parentReviewId == null) {
            return null;
        }
        if (parentReviewId <= 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "parentReviewId 必须为正整数");
        }
        ReviewRecordEntity parent = reviewRecordMapper.selectById(parentReviewId);
        if (parent == null || parent.getDeletedAt() != null || !sessionId.equals(parent.getSessionId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "父级评审记录不存在");
        }
        return parent;
    }

    private int nextVersionNo(String ideaGroupId, String sessionId, ReviewRecordEntity parent) {
        ReviewRecordEntity latest = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getSessionId, sessionId)
                .eq(ReviewRecordEntity::getIdeaGroupId, ideaGroupId)
                .isNull(ReviewRecordEntity::getDeletedAt)
                .orderByDesc(ReviewRecordEntity::getVersionNo)
                .last("LIMIT 1"));
        int latestVersion = latest == null ? 0 : safeVersionNo(latest);
        int parentVersion = parent == null ? 0 : safeVersionNo(parent);
        return Math.max(latestVersion, parentVersion) + 1;
    }

    private void ensureRecordGroupId(ReviewRecordEntity record) {
        if (record.getIdeaGroupId() != null && !record.getIdeaGroupId().isBlank()) {
            return;
        }
        record.setIdeaGroupId(String.valueOf(record.getId()));
        record.setUpdatedAt(LocalDateTime.now());
        reviewRecordMapper.updateById(record);
    }

    private String normalizeRecordGroupId(ReviewRecordEntity record) {
        if (record.getIdeaGroupId() != null && !record.getIdeaGroupId().isBlank()) {
            return record.getIdeaGroupId();
        }
        return record.getId() == null ? "" : String.valueOf(record.getId());
    }

    private int safeVersionNo(ReviewRecordEntity record) {
        return record.getVersionNo() == null || record.getVersionNo() <= 0 ? 1 : record.getVersionNo();
    }

    private int countGroupVersions(String ideaGroupId, String sessionId) {
        if (ideaGroupId == null || ideaGroupId.isBlank()) {
            return 1;
        }
        Long count = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getSessionId, sessionId)
                .eq(ReviewRecordEntity::getIdeaGroupId, ideaGroupId)
                .isNull(ReviewRecordEntity::getDeletedAt));
        int value = count == null ? 0 : count.intValue();
        if (value > 0) {
            return value;
        }
        return findLegacyGroupRecord(ideaGroupId, sessionId) == null ? 0 : 1;
    }

    private ReviewRecordEntity findLegacyGroupRecord(String ideaGroupId, String sessionId) {
        try {
            Long id = Long.valueOf(ideaGroupId);
            ReviewRecordEntity record = reviewRecordMapper.selectById(id);
            if (record == null || record.getDeletedAt() != null || !sessionId.equals(record.getSessionId())) {
                return null;
            }
            return record;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ReviewGroupResponse.ReviewVersionItem toReviewVersionItem(ReviewRecordEntity record) {
        ReviewReportDto report = readReport(record.getReportJson());
        ReviewReportDto.MinimumBuildVersion minimum = report.getMinimumBuildVersion();
        ReviewGroupResponse.ReviewVersionItem item = new ReviewGroupResponse.ReviewVersionItem();
        item.setId(record.getId());
        item.setVersionNo(safeVersionNo(record));
        item.setParentReviewId(record.getParentReviewId());
        item.setGoDecision(report.getGoDecision());
        item.setGoDecisionReason(report.getGoDecisionReason());
        item.setBeatScore(record.getBeatScore());
        item.setPositioningScore(record.getPositioningScore());
        item.setOneLineVerdict(record.getOneLineVerdict());
        item.setSuccessMetric(minimum == null ? "" : minimum.getSuccessMetric());
        item.setMinimumBuildGoal(minimum == null ? "" : minimum.getGoal());
        item.setCoreFeatures(minimum == null ? List.of() : minimum.getCoreFeatures());
        item.setExcludedFeatures(minimum == null ? List.of() : minimum.getExcludedFeatures());
        item.setValidationPlan(minimum == null ? List.of() : minimum.getValidationPlan());
        item.setCreatedAt(formatTime(record.getCreatedAt()));
        return item;
    }

    private ReviewDetailResponse toDetailResponse(ReviewRecordEntity record, ReviewReportDto report) {
        AiProvider.ProviderExecution execution = new AiProvider.ProviderExecution(
                safeProviderName(),
                record.getModelName(),
                false
        );
        return toDetailResponse(record, report, execution);
    }

    private ReviewDetailResponse toDetailResponse(
            ReviewRecordEntity record,
            ReviewReportDto report,
            AiProvider.ProviderExecution execution) {
        String ideaGroupId = normalizeRecordGroupId(record);
        ReviewDetailResponse response = new ReviewDetailResponse();
        response.setId(record.getId());
        response.setInputContent(record.getInputContent());
        response.setInputSummary(record.getInputSummary());
        response.setIdeaGroupId(ideaGroupId);
        response.setVersionNo(safeVersionNo(record));
        response.setParentReviewId(record.getParentReviewId());
        response.setGroupVersionCount(countGroupVersions(ideaGroupId, record.getSessionId()));
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
        response.setProviderName(execution.providerName());
        response.setModelName(execution.modelName());
        response.setFallbackUsed(execution.fallbackUsed());
        response.setCreatedAt(formatTime(record.getCreatedAt()));
        return response;
    }

    private ReviewListItemResponse toListItemResponse(ReviewRecordEntity record) {
        String ideaGroupId = normalizeRecordGroupId(record);
        ReviewListItemResponse response = new ReviewListItemResponse();
        response.setId(record.getId());
        response.setInputSummary(record.getInputSummary());
        response.setIdeaGroupId(ideaGroupId);
        response.setVersionNo(safeVersionNo(record));
        response.setParentReviewId(record.getParentReviewId());
        response.setGroupVersionCount(countGroupVersions(ideaGroupId, record.getSessionId()));
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
            String promptHash,
            long startedAt,
            BizException ex) {
        AiProvider.ProviderExecution execution = aiProvider.execution();
        log.debug(
                "review failed: provider={}, model={}, fallbackUsed={}, promptHash={}, errorCode={}",
                execution.providerName(),
                execution.modelName(),
                execution.fallbackUsed(),
                promptHash,
                ex.getErrorCode().getCode()
        );
        String safeMessage = sanitizeError(ex.getMessage());
        LocalDateTime failedAt = LocalDateTime.now();
        record.setStatus(ReviewStatus.FAILED.name());
        record.setErrorMessage(safeMessage);
        record.setModelName(execution.modelName());
        record.setUpdatedAt(failedAt);
        reviewRecordMapper.updateById(record);

        AiCallLogEntity log = new AiCallLogEntity();
        log.setReviewRecordId(record.getId());
        log.setProvider(execution.providerName());
        log.setModelName(record.getModelName());
        log.setPromptHash(promptHash);
        log.setRequestSummary("mode=%s, roastLevel=%d, contentLength=%d, contentPreview=%s, fallbackUsed=%s".formatted(
                record.getMode(),
                record.getRoastLevel(),
                record.getInputContent().length(),
                preview(record.getInputContent(), 80),
                execution.fallbackUsed()));
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

    private String normalizeIdeaGroupId(String ideaGroupId) {
        String value = ideaGroupId == null ? "" : ideaGroupId.trim();
        if (value.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "ideaGroupId 不能为空");
        }
        if (value.length() > 64) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "ideaGroupId 不能超过 64 个字符");
        }
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "ideaGroupId 只能包含字母、数字、下划线或短横线");
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildSummary(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
    }

    private String preview(String content, int maxLength) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
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
            ReviewReportDto report,
            AiProvider.ProviderExecution execution) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 猪猪产品毒舌官评审报告\n\n");
        builder.append("> 评审来源：").append(providerLabel(execution)).append("\n\n");
        builder.append("## 1. 产品决策区\n\n");
        builder.append("- 评审对象：").append(toReviewTargetTypeLabel(report.getReviewTargetType())).append("\n");
        builder.append("- 决策结论：").append(toDecisionLabel(report.getGoDecision())).append("\n");
        builder.append("- 一句话结论：").append(report.getOneLineVerdict()).append("\n");
        builder.append("- 决策原因：").append(report.getGoDecisionReason()).append("\n");
        builder.append("- 毒打指数：").append(report.getBeatScore()).append("/100\n");
        builder.append("- 产品定位评分：").append(report.getPositioningScore()).append("/100\n");
        builder.append("- 成功指标：").append(report.getMinimumBuildVersion().getSuccessMetric()).append("\n\n");
        builder.append("## 2. 用户痛点分析\n\n").append(report.getPainPointAnalysis()).append("\n\n");
        appendList(builder, "## 3. 伪需求风险", report.getFakeDemandRisks());
        appendList(builder, "## 4. 功能冗余检查", report.getFeatureRedundancyCheck());
        appendList(builder, "## 5. 冷启动问题", report.getColdStartProblems());
        appendList(builder, "## 6. MVP 改造建议", report.getMvpSuggestions());
        builder.append("## 7. 最小可开发版本\n\n");
        builder.append("目标：").append(report.getMinimumBuildVersion().getGoal()).append("\n\n");
        appendList(builder, "核心功能", report.getMinimumBuildVersion().getCoreFeatures());
        appendList(builder, "暂不实现", report.getMinimumBuildVersion().getExcludedFeatures());
        appendList(builder, "## 8. 验证计划", report.getMinimumBuildVersion().getValidationPlan());
        builder.append("## 9. Codex/Cursor 开发 Prompt\n\n");
        builder.append("```markdown\n").append(report.getDeveloperPrompt()).append("\n```\n\n");
        builder.append("---\n\n");
        builder.append("评审模式：").append(mode.name()).append("\n\n");
        builder.append("吐槽强度：").append(roastLevel).append("\n\n");
        builder.append("原始输入摘要：").append(buildSummary(content)).append("\n");
        return builder.toString();
    }

    private String providerLabel(AiProvider.ProviderExecution execution) {
        if (execution.fallbackUsed()) {
            return "Fallback / " + execution.providerName() + " / " + execution.modelName();
        }
        if ("mock".equalsIgnoreCase(execution.providerName())) {
            return "Mock / " + execution.modelName();
        }
        return execution.providerName() + " / " + execution.modelName();
    }

    private String toDecisionLabel(String decision) {
        return switch (decision) {
            case "PIVOT" -> "建议调整方向";
            case "PAUSE" -> "建议暂缓";
            default -> "建议继续";
        };
    }

    private String toReviewTargetTypeLabel(String type) {
        return switch (type == null ? "" : type) {
            case "NEW_IDEA" -> "新产品想法";
            case "MATURE_PRODUCT" -> "成熟产品复盘";
            case "CLIENT_REQUIREMENT" -> "甲方需求";
            case "UNCLEAR" -> "输入不清晰";
            default -> "未识别";
        };
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
