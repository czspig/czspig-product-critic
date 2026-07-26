package com.czspig.productcritic.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.czspig.productcritic.ai.AiProvider;
import com.czspig.productcritic.ai.ProductReviewOutputParser;
import com.czspig.productcritic.ai.ProductReviewPromptBuilder;
import com.czspig.productcritic.ai.ReviewReportRuleEngine;
import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.common.PageResponse;
import com.czspig.productcritic.common.ReviewMode;
import com.czspig.productcritic.common.ReviewStatus;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ProjectWorkbenchResponse;
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
    private final ReviewReportRuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    public ReviewServiceImpl(
            ReviewRecordMapper reviewRecordMapper,
            AiCallLogMapper aiCallLogMapper,
            AiProvider aiProvider,
            ProductReviewPromptBuilder promptBuilder,
            ProductReviewOutputParser outputParser,
            ReviewReportRuleEngine ruleEngine,
            ObjectMapper objectMapper) {
        this.reviewRecordMapper = reviewRecordMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.aiProvider = aiProvider;
        this.promptBuilder = promptBuilder;
        this.outputParser = outputParser;
        this.ruleEngine = ruleEngine;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(noRollbackFor = BizException.class)
    public ReviewDetailResponse createReview(CreateReviewRequest request, String sessionId) {
        ReviewMode mode = ReviewMode.requireValid(request.getMode());
        String safeContent = normalizeContent(request.getContent());
        String safeSessionId = normalizeSessionId(sessionId);
        int safeRoastLevel = normalizeRoastLevel(request.getRoastLevel());

        CreateReviewRequest normalizedRequest = new CreateReviewRequest();
        normalizedRequest.setContent(safeContent);
        normalizedRequest.setMode(mode.name());
        normalizedRequest.setRoastLevel(safeRoastLevel);
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
        record.setRoastLevel(safeRoastLevel);
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
            ruleEngine.normalizeTrustedReview(report);
            ruleEngine.audit(report);
            outputParser.validate(report);

            String reportJson = writeJson(report);
            String reportMarkdown = renderTrustedMarkdown(safeContent, mode, safeRoastLevel, report, execution);
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
        List<ReviewRecordEntity> records = findGroupRecords(safeGroupId, safeSessionId);
        if (records.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "迭代版本不存在");
        }

        ReviewGroupResponse response = new ReviewGroupResponse();
        response.setIdeaGroupId(safeGroupId);
        response.setVersions(records.stream().map(this::toReviewVersionItem).toList());
        return response;
    }

    @Override
    public ProjectWorkbenchResponse getProjectWorkbench(String ideaGroupId, String sessionId) {
        String safeSessionId = normalizeSessionId(sessionId);
        String safeGroupId = normalizeIdeaGroupId(ideaGroupId);
        List<ReviewRecordEntity> records = findGroupRecords(safeGroupId, safeSessionId);
        if (records.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }

        List<ReviewRecordEntity> orderedRecords = records.stream()
                .sorted(projectVersionComparator())
                .toList();
        ReviewRecordEntity latest = orderedRecords.get(orderedRecords.size() - 1);
        ReviewReportDto report = readReport(latest.getReportJson());
        ReviewReportDto.KnowledgeBoundary boundary = report.getKnowledgeBoundary();
        ReviewReportDto.MinimumBuildVersion minimum = report.getMinimumBuildVersion();
        ReviewReportDto.OverallAssessment assessment = report.getOverallAssessment();
        ReviewReportDto.QualityAudit quality = report.getQualityAudit();

        ProjectWorkbenchResponse response = new ProjectWorkbenchResponse();
        response.setProjectId(safeGroupId);
        response.setTitle(latest.getInputSummary());
        response.setLatestReviewId(latest.getId());
        response.setLatestVersionNo(safeVersionNo(latest));
        response.setUpdatedAt(formatTime(latest.getUpdatedAt() == null ? latest.getCreatedAt() : latest.getUpdatedAt()));
        response.setTotalVersions(orderedRecords.size());
        response.setDecision(report.getGoDecision());
        response.setDecisionReason(report.getGoDecisionReason());
        response.setOneLineVerdict(latest.getOneLineVerdict());
        response.setBeatScore(latest.getBeatScore());
        response.setPositioningScore(latest.getPositioningScore());
        response.setOverallScore(assessment == null ? null : assessment.getOverallScore());
        response.setQualityStatus(quality == null ? null : quality.getOverallStatus());
        response.setQualitySummary(quality == null ? null : quality.getSummary());
        response.setProvidedFacts(boundary == null ? List.of() : safeList(boundary.getProvidedFacts()));
        response.setInferences(boundary == null ? List.of() : safeList(boundary.getInferences()));
        response.setCriticalHypotheses(boundary == null ? List.of() : safeList(boundary.getCriticalHypotheses()));
        response.setUnknownQuestions(boundary == null ? List.of() : safeList(boundary.getUnknowns()));
        response.setSuccessMetric(minimum == null ? null : minimum.getSuccessMetric());
        response.setValidationPlan(minimum == null ? List.of() : safeList(minimum.getValidationPlan()));
        response.setVersions(orderedRecords.stream().map(this::toReviewVersionItem).toList());
        return response;
    }

    private List<ReviewRecordEntity> findGroupRecords(String ideaGroupId, String sessionId) {
        List<ReviewRecordEntity> records = reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getSessionId, sessionId)
                .eq(ReviewRecordEntity::getIdeaGroupId, ideaGroupId)
                .isNull(ReviewRecordEntity::getDeletedAt)
                .orderByAsc(ReviewRecordEntity::getVersionNo)
                .orderByAsc(ReviewRecordEntity::getCreatedAt));
        if (!records.isEmpty()) {
            return records;
        }
        ReviewRecordEntity legacyRecord = findLegacyGroupRecord(ideaGroupId, sessionId);
        return legacyRecord == null ? List.of() : List.of(legacyRecord);
    }

    private Comparator<ReviewRecordEntity> projectVersionComparator() {
        return Comparator.comparingInt(this::safeVersionNo)
                .thenComparing(ReviewRecordEntity::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ReviewRecordEntity::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(record -> record.getId() == null ? 0L : record.getId());
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
        item.setCoreFeatures(minimum == null ? List.of() : safeList(minimum.getCoreFeatures()));
        item.setExcludedFeatures(minimum == null ? List.of() : safeList(minimum.getExcludedFeatures()));
        item.setValidationPlan(minimum == null ? List.of() : safeList(minimum.getValidationPlan()));
        item.setScoreChangeReason(firstDimensionChangeReason(report));
        item.setConfidenceSummary(confidenceSummary(report));
        item.setCriticalHypotheses(criticalHypotheses(report));
        item.setQualityStatus(report.getQualityAudit() == null ? null : report.getQualityAudit().getOverallStatus());
        item.setCreatedAt(formatTime(record.getCreatedAt()));
        return item;
    }

    private String firstDimensionChangeReason(ReviewReportDto report) {
        if (report.getDimensionScores() == null || report.getDimensionScores().isEmpty()) {
            return null;
        }
        return report.getDimensionScores().stream()
                .map(ReviewReportDto.DimensionScore::getChangeReason)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String confidenceSummary(ReviewReportDto report) {
        if (report.getDimensionScores() == null || report.getDimensionScores().isEmpty()) {
            return null;
        }
        long high = report.getDimensionScores().stream().filter(item -> "HIGH".equals(item.getConfidence())).count();
        long medium = report.getDimensionScores().stream().filter(item -> "MEDIUM".equals(item.getConfidence())).count();
        long low = report.getDimensionScores().stream().filter(item -> "LOW".equals(item.getConfidence())).count();
        return "高 " + high + " / 中 " + medium + " / 低 " + low;
    }

    private List<String> criticalHypotheses(ReviewReportDto report) {
        if (report.getKnowledgeBoundary() == null || report.getKnowledgeBoundary().getCriticalHypotheses() == null) {
            return List.of();
        }
        return report.getKnowledgeBoundary().getCriticalHypotheses().stream()
                .map(ReviewReportDto.CriticalHypothesis::getStatement)
                .filter(value -> value != null && !value.isBlank())
                .limit(3)
                .toList();
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

    private int normalizeRoastLevel(Integer roastLevel) {
        return roastLevel == null ? 2 : roastLevel;
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

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
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

    private String renderTrustedMarkdown(
            String content,
            ReviewMode mode,
            Integer roastLevel,
            ReviewReportDto report,
            AiProvider.ProviderExecution execution) {
        StringBuilder builder = new StringBuilder();
        ReviewReportDto.MinimumBuildVersion minimum = report.getMinimumBuildVersion();
        ReviewReportDto.OverallAssessment overall = report.getOverallAssessment();
        ReviewReportDto.KnowledgeBoundary boundary = report.getKnowledgeBoundary();
        ReviewReportDto.QualityAudit audit = report.getQualityAudit();

        builder.append("# 猪猪产品毒蛇官可信评审报告\n\n");
        builder.append("> 评审来源：").append(providerLabel(execution)).append("\n\n");

        builder.append("## 1. 一句话结论\n\n");
        builder.append("- 总体判断：").append(overall == null ? trustedDecisionLabel(report.getGoDecision()) : overallDecisionLabel(overall.getDecision())).append("\n");
        builder.append("- 一句话毒舌结论：").append(report.getOneLineVerdict()).append("\n");
        builder.append("- 下一步建议：").append(overall == null ? report.getGoDecisionReason() : overall.getPriorityQuestion()).append("\n\n");

        builder.append("## 2. 总体判断\n\n");
        builder.append("- 评审对象：").append(trustedTargetTypeLabel(report.getReviewTargetType())).append("\n");
        builder.append("- 旧版决策：").append(trustedDecisionLabel(report.getGoDecision())).append("\n");
        builder.append("- 毒打指数：").append(report.getBeatScore()).append("/100\n");
        builder.append("- 产品定位评分：").append(report.getPositioningScore()).append("/100\n");
        if (overall != null) {
            builder.append("- 可信总分：").append(overall.getOverallScore()).append("/100\n");
            builder.append("- 最强维度：").append(overall.getStrongestDimension()).append("\n");
            builder.append("- 最弱维度：").append(overall.getWeakestDimension()).append("\n");
            builder.append("- 最不确定维度：").append(overall.getMostUncertainDimension()).append("\n");
            builder.append("- 判断理由：").append(overall.getOverallReasoning()).append("\n");
        }
        builder.append("\n");

        builder.append("## 3. 多维评分与证据\n\n");
        if (report.getDimensionScores() == null || report.getDimensionScores().isEmpty()) {
            builder.append("该报告生成于可信评审功能上线前，暂无评分证据链。\n\n");
        } else {
            for (ReviewReportDto.DimensionScore dimension : report.getDimensionScores()) {
                builder.append("### ").append(dimension.getDimensionName()).append("\n\n");
                builder.append("- 分数：").append(dimension.getScore()).append("/100\n");
                builder.append("- 置信度：").append(confidenceLabel(dimension.getConfidence())).append("\n");
                builder.append("- 方向：").append(scoreDirectionLabel(dimension.getScoreDirection())).append("\n");
                builder.append("- 摘要：").append(dimension.getSummary()).append("\n\n");
                appendMarkdownList(builder, "得分依据", dimension.getEvidence());
                appendMarkdownList(builder, "主要扣分项", dimension.getDeductions());
                appendMarkdownList(builder, "缺失信息", dimension.getMissingInformation());
                appendMarkdownList(builder, "提升建议", dimension.getImprovementActions());
            }
        }

        builder.append("## 4. 用户已提供事实\n\n");
        if (boundary == null || boundary.getProvidedFacts() == null || boundary.getProvidedFacts().isEmpty()) {
            builder.append("- 暂无可定位事实。\n\n");
        } else {
            for (ReviewReportDto.ProvidedFact fact : boundary.getProvidedFacts()) {
                builder.append("- ").append(fact.getStatement()).append("（原文：").append(fact.getSourceText()).append("）\n");
            }
            builder.append("\n");
        }

        builder.append("## 5. AI 推测\n\n");
        if (boundary == null || boundary.getInferences() == null || boundary.getInferences().isEmpty()) {
            builder.append("- 暂无推测。\n\n");
        } else {
            for (ReviewReportDto.Inference inference : boundary.getInferences()) {
                builder.append("- ").append(inference.getStatement())
                        .append("（").append(confidenceLabel(inference.getConfidence()))
                        .append("，依据：").append(inference.getBasis()).append("）\n");
            }
            builder.append("\n");
        }

        builder.append("## 6. 关键待验证假设\n\n");
        if (boundary == null || boundary.getCriticalHypotheses() == null || boundary.getCriticalHypotheses().isEmpty()) {
            builder.append("- 暂无关键假设。\n\n");
        } else {
            for (ReviewReportDto.CriticalHypothesis hypothesis : boundary.getCriticalHypotheses()) {
                builder.append("- ").append(hypothesis.getStatement())
                        .append("（优先级：").append(confidenceLabel(hypothesis.getImportance()))
                        .append("，若不成立：").append(hypothesis.getImpactIfFalse()).append("）\n");
            }
            builder.append("\n");
        }

        builder.append("## 7. 当前未知问题\n\n");
        if (boundary == null || boundary.getUnknowns() == null || boundary.getUnknowns().isEmpty()) {
            builder.append("- 暂无未知问题。\n\n");
        } else {
            for (ReviewReportDto.UnknownQuestion unknown : boundary.getUnknowns()) {
                builder.append("- ").append(unknown.getQuestion()).append("：").append(unknown.getReason()).append("\n");
            }
            builder.append("\n");
        }

        builder.append("## 8. 结构化评审报告\n\n");
        builder.append("### 用户痛点分析\n\n").append(report.getPainPointAnalysis()).append("\n\n");
        appendMarkdownList(builder, "伪需求风险", report.getFakeDemandRisks());
        appendMarkdownList(builder, "功能冗余检查", report.getFeatureRedundancyCheck());
        appendMarkdownList(builder, "冷启动问题", report.getColdStartProblems());

        builder.append("## 9. MVP 建议\n\n");
        appendMarkdownList(builder, "MVP 改造建议", report.getMvpSuggestions());
        builder.append("### 最小可开发版本\n\n");
        builder.append("- 目标：").append(minimum.getGoal()).append("\n");
        builder.append("- 成功指标：").append(minimum.getSuccessMetric()).append("\n\n");
        appendMarkdownList(builder, "核心功能", minimum.getCoreFeatures());
        appendMarkdownList(builder, "暂不实现", minimum.getExcludedFeatures());

        builder.append("## 10. 验证计划\n\n");
        appendMarkdownList(builder, "验证动作", minimum.getValidationPlan());

        builder.append("## 11. 报告质量检查\n\n");
        if (audit == null) {
            builder.append("该报告暂无质量自检结果。\n\n");
        } else {
            builder.append("- 总体状态：").append(qualityStatusLabel(audit.getOverallStatus())).append("\n");
            builder.append("- 摘要：").append(audit.getSummary()).append("\n\n");
            if (audit.getChecks() != null) {
                for (ReviewReportDto.QualityCheck check : audit.getChecks()) {
                    builder.append("- ").append(check.getName()).append("：")
                            .append(qualityStatusLabel(check.getStatus()))
                            .append("，").append(check.getMessage()).append("\n");
                }
                builder.append("\n");
            }
        }

        builder.append("## 12. 开发任务 Prompt\n\n");
        builder.append("```markdown\n").append(report.getDeveloperPrompt()).append("\n```\n\n");
        builder.append("---\n\n");
        builder.append("评审模式：").append(mode.name()).append("\n\n");
        builder.append("毒舌强度：").append(roastLevel).append("\n\n");
        builder.append("原始输入摘要：").append(buildSummary(content)).append("\n");
        return builder.toString();
    }

    private void appendMarkdownList(StringBuilder builder, String title, List<String> values) {
        builder.append("### ").append(title).append("\n\n");
        if (values == null || values.isEmpty()) {
            builder.append("- 暂无\n\n");
            return;
        }
        for (String value : values) {
            builder.append("- ").append(value).append("\n");
        }
        builder.append("\n");
    }

    private String overallDecisionLabel(String decision) {
        return switch (decision == null ? "" : decision) {
            case "PROCEED" -> "值得做";
            case "NARROW_SCOPE" -> "先缩小范围";
            case "PAUSE" -> "不建议投入";
            default -> "谨慎验证";
        };
    }

    private String trustedDecisionLabel(String decision) {
        return switch (decision == null ? "" : decision) {
            case "PIVOT" -> "建议调整方向";
            case "PAUSE" -> "建议暂缓";
            default -> "建议继续";
        };
    }

    private String trustedTargetTypeLabel(String type) {
        return switch (type == null ? "" : type) {
            case "NEW_IDEA" -> "新产品想法";
            case "MATURE_PRODUCT" -> "成熟产品复盘";
            case "CLIENT_REQUIREMENT" -> "甲方需求";
            case "UNCLEAR" -> "输入不清晰";
            default -> "未识别";
        };
    }

    private String confidenceLabel(String value) {
        return switch (value == null ? "" : value) {
            case "HIGH" -> "高";
            case "LOW" -> "低";
            default -> "中";
        };
    }

    private String scoreDirectionLabel(String value) {
        return "HIGHER_IS_WORSE".equals(value) ? "分数越高，问题越重" : "分数越高，表现越好";
    }

    private String qualityStatusLabel(String value) {
        return switch (value == null ? "" : value) {
            case "PASS" -> "通过";
            case "FAIL" -> "未通过";
            default -> "需注意";
        };
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
