package com.czspig.productcritic.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ProductReviewOutputParser {

    private final ReviewReportRuleEngine ruleEngine;

    public ProductReviewOutputParser(ReviewReportRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public ReviewReportDto parseAndValidate(String rawContent, ObjectMapper objectMapper) {
        try {
            ReviewReportDto report = objectMapper.readValue(extractJsonObject(rawContent), ReviewReportDto.class);
            normalize(report);
            ruleEngine.normalizeTrustedReview(report);
            ruleEngine.audit(report);
            validate(report);
            return report;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 输出结构解析失败");
        }
    }

    public ReviewReportDto parseStrictAndValidate(String rawContent, ObjectMapper objectMapper) {
        String json = extractJsonObject(rawContent);
        ReviewReportDto report;
        try {
            report = objectMapper.readValue(json, ReviewReportDto.class);
        } catch (JsonProcessingException ex) {
            throw parseFailure("INVALID_JSON", "AI 输出不是合法 JSON 对象");
        }
        normalizeNullLists(report);
        normalizeEnums(report);
        validateTrustedStructureBeforeFallback(report);
        normalize(report);
        ruleEngine.normalizeTrustedReview(report);
        ruleEngine.audit(report);
        validate(report);
        return report;
    }

    public void validate(ReviewReportDto report) {
        if (report == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "评审报告为空");
        }
        requireText(report.getOneLineVerdict(), "一句话评价不能为空");
        requireReviewTargetType(report.getReviewTargetType());
        requireGoDecision(report.getGoDecision());
        requireText(report.getGoDecisionReason(), "是否继续做的理由不能为空");
        requireScore(report.getBeatScore(), "毒打指数");
        requireScore(report.getPositioningScore(), "产品定位评分");
        requireText(report.getPainPointAnalysis(), "用户痛点分析不能为空");
        requireList(report.getFakeDemandRisks(), "伪需求风险不能为空");
        requireList(report.getFeatureRedundancyCheck(), "功能冗余检查不能为空");
        requireList(report.getColdStartProblems(), "冷启动问题不能为空");
        requireList(report.getMvpSuggestions(), "MVP 改造建议不能为空");
        requireList(report.getDimensionScores(), "评分证据链不能为空");
        if (report.getKnowledgeBoundary() == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "事实与假设边界不能为空");
        }
        if (report.getOverallAssessment() == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "总体评分解释不能为空");
        }
        if (report.getQualityAudit() == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "报告质量检查不能为空");
        }
        if (report.getMinimumBuildVersion() == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "最小可开发版本不能为空");
        }
        requireText(report.getMinimumBuildVersion().getGoal(), "最小可开发版本目标不能为空");
        requireList(report.getMinimumBuildVersion().getCoreFeatures(), "最小可开发版本核心功能不能为空");
        requireList(report.getMinimumBuildVersion().getExcludedFeatures(), "最小可开发版本排除功能不能为空");
        requireText(report.getMinimumBuildVersion().getSuccessMetric(), "最小可开发版本成功指标不能为空");
        requireList(report.getMinimumBuildVersion().getValidationPlan(), "最小可开发版本验证计划不能为空");
        requireText(report.getDeveloperPrompt(), "开发 Prompt 不能为空");
    }

    private void normalize(ReviewReportDto report) {
        if (report == null) {
            return;
        }
        report.setReviewTargetType(validReviewTargetType(report.getReviewTargetType()) ? report.getReviewTargetType() : "UNCLEAR");
        report.setGoDecision(validGoDecision(report.getGoDecision()) ? report.getGoDecision() : "PAUSE");
        report.setBeatScore(clamp(report.getBeatScore(), 75));
        report.setPositioningScore(clamp(report.getPositioningScore(), 35));
        report.setOneLineVerdict(defaultText(
                report.getOneLineVerdict(),
                "当前输入还需要收缩目标用户、使用场景和最小验证闭环。"
        ));
        report.setGoDecisionReason(defaultText(
                report.getGoDecisionReason(),
                "模型输出缺少决策理由，系统已兜底为暂停推进：请先补充目标用户、场景、风险和成功指标。"
        ));
        report.setPainPointAnalysis(defaultText(
                report.getPainPointAnalysis(),
                "当前报告缺少痛点分析。请先明确目标用户、痛点频率、替代方案和用户为什么现在必须解决。"
        ));
        report.setFakeDemandRisks(defaultList(
                report.getFakeDemandRisks(),
                "可能存在用户嘴上说需要、实际不愿持续使用的伪需求风险。",
                "请用 5-10 个目标用户访谈或手工交付实验验证核心假设。"
        ));
        report.setFeatureRedundancyCheck(defaultList(
                report.getFeatureRedundancyCheck(),
                "第一版应砍掉登录支付、复杂后台、社区、分享裂变等非核心功能。",
                "只保留能验证一次核心任务闭环的最小功能。"
        ));
        report.setColdStartProblems(defaultList(
                report.getColdStartProblems(),
                "第一批用户来源、触达方式和使用动机仍需明确。",
                "如果依赖内容、数据或供需双方，请先用人工方式验证冷启动路径。"
        ));
        report.setMvpSuggestions(defaultList(
                report.getMvpSuggestions(),
                "把第一版收缩到一个目标用户、一个高频场景和一次可完成的核心任务。",
                "用可观察指标判断是否继续开发，而不是先堆功能。"
        ));

        if (report.getMinimumBuildVersion() == null) {
            report.setMinimumBuildVersion(new ReviewReportDto.MinimumBuildVersion());
        }
        ReviewReportDto.MinimumBuildVersion minimum = report.getMinimumBuildVersion();
        minimum.setGoal(defaultText(minimum.getGoal(), "验证目标用户是否愿意完成一次核心任务并留下可复用结果。"));
        minimum.setCoreFeatures(defaultList(
                minimum.getCoreFeatures(),
                "产品想法输入",
                "结构化评审报告",
                "历史记录查看"
        ));
        minimum.setExcludedFeatures(defaultList(
                minimum.getExcludedFeatures(),
                "登录支付",
                "分享裂变",
                "复杂后台"
        ));
        minimum.setSuccessMetric(defaultText(
                minimum.getSuccessMetric(),
                "至少 5 位目标用户完成核心流程，并愿意基于结果继续调整想法。"
        ));
        minimum.setValidationPlan(defaultList(
                minimum.getValidationPlan(),
                "找 5-10 位目标用户访谈。",
                "用手工或最小页面交付一次核心结果。",
                "记录是否愿意再次使用或付出迁移成本。"
        ));
        report.setDeveloperPrompt(defaultText(
                report.getDeveloperPrompt(),
                "请基于现有仓库实现最小可运行版本，只保留核心输入、评审结果和历史查看，不做登录、支付、分享裂变或复杂后台。"
        ));
    }

    private void validateTrustedStructureBeforeFallback(ReviewReportDto report) {
        if (report == null) {
            throw parseFailure("SCHEMA_VALIDATION_FAILED", "评审报告为空");
        }
        if (report.getOverallAssessment() == null) {
            throw parseFailure("SCHEMA_VALIDATION_FAILED", "overallAssessment 缺失");
        }
        if (report.getKnowledgeBoundary() == null) {
            throw parseFailure("SCHEMA_VALIDATION_FAILED", "knowledgeBoundary 缺失");
        }
        if (report.getQualityAudit() == null) {
            throw parseFailure("SCHEMA_VALIDATION_FAILED", "qualityAudit 缺失");
        }
        if (report.getMinimumBuildVersion() == null) {
            throw parseFailure("SCHEMA_VALIDATION_FAILED", "minimumBuildVersion 缺失");
        }
        List<ReviewReportDto.DimensionScore> dimensions = report.getDimensionScores();
        if (dimensions == null || dimensions.size() != 6) {
            throw parseFailure("SCHEMA_VALIDATION_FAILED", "dimensionScores 数量为 "
                    + (dimensions == null ? 0 : dimensions.size()) + "，预期 6");
        }
        Set<String> seen = new HashSet<>();
        for (ReviewReportDto.DimensionScore dimension : dimensions) {
            String key = dimension.getDimensionKey();
            if (!Set.of("painPoint", "marketSpace", "businessModel", "implementationDifficulty", "growthPotential", "riskLevel").contains(key)) {
                throw parseFailure("SCHEMA_VALIDATION_FAILED", "dimensionScores 包含非法 dimensionKey: " + key);
            }
            if (!seen.add(key)) {
                throw parseFailure("SCHEMA_VALIDATION_FAILED", "dimensionScores 包含重复维度: " + key);
            }
            if (!validConfidence(dimension.getConfidence())) {
                throw parseFailure("SCHEMA_VALIDATION_FAILED", "confidence 的值 " + dimension.getConfidence() + " 不合法，允许 HIGH/MEDIUM/LOW");
            }
            if (!validScoreDirection(key, dimension.getScoreDirection())) {
                throw parseFailure("SCHEMA_VALIDATION_FAILED", "scoreDirection 的值 " + dimension.getScoreDirection() + " 与 " + key + " 不匹配");
            }
        }
    }

    private void normalizeNullLists(ReviewReportDto report) {
        if (report == null) {
            return;
        }
        if (report.getFakeDemandRisks() == null) {
            report.setFakeDemandRisks(new ArrayList<>());
        }
        if (report.getFeatureRedundancyCheck() == null) {
            report.setFeatureRedundancyCheck(new ArrayList<>());
        }
        if (report.getColdStartProblems() == null) {
            report.setColdStartProblems(new ArrayList<>());
        }
        if (report.getMvpSuggestions() == null) {
            report.setMvpSuggestions(new ArrayList<>());
        }
        if (report.getDimensionScores() == null) {
            report.setDimensionScores(new ArrayList<>());
        }
        report.getDimensionScores().forEach(item -> {
            if (item.getEvidence() == null) {
                item.setEvidence(new ArrayList<>());
            }
            if (item.getDeductions() == null) {
                item.setDeductions(new ArrayList<>());
            }
            if (item.getMissingInformation() == null) {
                item.setMissingInformation(new ArrayList<>());
            }
            if (item.getImprovementActions() == null) {
                item.setImprovementActions(new ArrayList<>());
            }
        });
        if (report.getMinimumBuildVersion() != null) {
            ReviewReportDto.MinimumBuildVersion minimum = report.getMinimumBuildVersion();
            if (minimum.getCoreFeatures() == null) {
                minimum.setCoreFeatures(new ArrayList<>());
            }
            if (minimum.getExcludedFeatures() == null) {
                minimum.setExcludedFeatures(new ArrayList<>());
            }
            if (minimum.getValidationPlan() == null) {
                minimum.setValidationPlan(new ArrayList<>());
            }
        }
        if (report.getKnowledgeBoundary() != null) {
            ReviewReportDto.KnowledgeBoundary boundary = report.getKnowledgeBoundary();
            if (boundary.getProvidedFacts() == null) {
                boundary.setProvidedFacts(new ArrayList<>());
            }
            if (boundary.getInferences() == null) {
                boundary.setInferences(new ArrayList<>());
            }
            if (boundary.getCriticalHypotheses() == null) {
                boundary.setCriticalHypotheses(new ArrayList<>());
            }
            if (boundary.getUnknowns() == null) {
                boundary.setUnknowns(new ArrayList<>());
            }
            boundary.getCriticalHypotheses().forEach(item -> {
                if (item.getRelatedDimensions() == null) {
                    item.setRelatedDimensions(new ArrayList<>());
                }
            });
        }
        if (report.getQualityAudit() != null) {
            ReviewReportDto.QualityAudit audit = report.getQualityAudit();
            if (audit.getChecks() == null) {
                audit.setChecks(new ArrayList<>());
            }
            if (audit.getIssues() == null) {
                audit.setIssues(new ArrayList<>());
            }
            audit.getChecks().forEach(item -> {
                if (item.getRelatedPaths() == null) {
                    item.setRelatedPaths(new ArrayList<>());
                }
            });
        }
    }

    private void normalizeEnums(ReviewReportDto report) {
        report.setReviewTargetType(upperIfAllowed(report.getReviewTargetType(), Set.of("NEW_IDEA", "MATURE_PRODUCT", "CLIENT_REQUIREMENT", "UNCLEAR")));
        report.setGoDecision(upperIfAllowed(report.getGoDecision(), Set.of("CONTINUE", "PIVOT", "PAUSE")));
        if (report.getOverallAssessment() != null) {
            report.getOverallAssessment().setDecision(upperIfAllowed(
                    report.getOverallAssessment().getDecision(),
                    Set.of("PROCEED", "VALIDATE_FIRST", "NARROW_SCOPE", "PAUSE")
            ));
        }
        report.getDimensionScores().forEach(item -> {
            item.setConfidence(upperIfAllowed(item.getConfidence(), Set.of("HIGH", "MEDIUM", "LOW")));
            item.setScoreDirection(upperIfAllowed(item.getScoreDirection(), Set.of("HIGHER_IS_BETTER", "HIGHER_IS_WORSE")));
        });
        if (report.getKnowledgeBoundary() != null) {
            report.getKnowledgeBoundary().getInferences().forEach(item ->
                    item.setConfidence(upperIfAllowed(item.getConfidence(), Set.of("HIGH", "MEDIUM", "LOW"))));
            report.getKnowledgeBoundary().getCriticalHypotheses().forEach(item ->
                    item.setImportance(upperIfAllowed(item.getImportance(), Set.of("HIGH", "MEDIUM", "LOW"))));
        }
        if (report.getQualityAudit() != null) {
            report.getQualityAudit().setOverallStatus(upperIfAllowed(report.getQualityAudit().getOverallStatus(), Set.of("PASS", "WARNING", "FAIL")));
            report.getQualityAudit().getChecks().forEach(item ->
                    item.setStatus(upperIfAllowed(item.getStatus(), Set.of("PASS", "WARNING", "FAIL"))));
            report.getQualityAudit().getIssues().forEach(item ->
                    item.setSeverity(upperIfAllowed(item.getSeverity(), Set.of("HIGH", "MEDIUM", "LOW"))));
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, message);
        }
    }

    private void requireScore(Integer value, String fieldName) {
        if (value == null || value < 0 || value > 100) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, fieldName + "必须在 0-100 之间");
        }
    }

    private void requireGoDecision(String value) {
        if (!validGoDecision(value)) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "是否继续做必须是 CONTINUE、PIVOT 或 PAUSE");
        }
    }

    private void requireReviewTargetType(String value) {
        if (!validReviewTargetType(value)) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "评审对象类型必须是 NEW_IDEA、MATURE_PRODUCT、CLIENT_REQUIREMENT 或 UNCLEAR");
        }
    }

    private void requireList(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, message);
        }
    }

    private boolean validGoDecision(String value) {
        return "CONTINUE".equals(value) || "PIVOT".equals(value) || "PAUSE".equals(value);
    }

    private boolean validReviewTargetType(String value) {
        return "NEW_IDEA".equals(value)
                || "MATURE_PRODUCT".equals(value)
                || "CLIENT_REQUIREMENT".equals(value)
                || "UNCLEAR".equals(value);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> defaultList(List<String> values, String... fallback) {
        if (values == null) {
            return new ArrayList<>(List.of(fallback));
        }
        List<String> cleaned = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return cleaned.isEmpty() ? new ArrayList<>(List.of(fallback)) : new ArrayList<>(cleaned);
    }

    private Integer clamp(Integer value, int fallback) {
        int safe = value == null ? fallback : value;
        return Math.max(0, Math.min(100, safe));
    }

    String extractJsonObject(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw parseFailure("EMPTY_RESPONSE", "AI 输出为空");
        }
        String normalized = stripBom(rawContent).strip();
        if (normalized.startsWith("```")) {
            normalized = normalized
                    .replaceFirst("^```(?:json|JSON)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .strip();
        }
        if (normalized.startsWith("[")) {
            throw parseFailure("INVALID_JSON", "AI 输出顶层是数组，预期 JSON 对象");
        }
        int start = normalized.indexOf('{');
        if (start < 0) {
            throw parseFailure("INVALID_JSON", "AI 输出不是 JSON 对象");
        }
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return normalized.substring(start, i + 1);
                }
                if (depth < 0) {
                    throw parseFailure("INVALID_JSON", "AI 输出 JSON 大括号不匹配");
                }
            }
        }
        if (depth > 0) {
            throw parseFailure("OUTPUT_TRUNCATED", "AI 输出疑似被截断，顶层 JSON 对象没有闭合");
        }
        throw parseFailure("INVALID_JSON", "AI 输出不是完整 JSON 对象");
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private String upperIfAllowed(String value, Set<String> allowed) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return allowed.contains(normalized) ? normalized : value;
    }

    private boolean validConfidence(String value) {
        return Set.of("HIGH", "MEDIUM", "LOW").contains(value);
    }

    private boolean validScoreDirection(String key, String direction) {
        if ("implementationDifficulty".equals(key) || "riskLevel".equals(key)) {
            return "HIGHER_IS_WORSE".equals(direction);
        }
        return "HIGHER_IS_BETTER".equals(direction);
    }

    private OutputParseException parseFailure(String type, String message) {
        return new OutputParseException(type, message);
    }

    public static class OutputParseException extends BizException {

        private final String type;

        OutputParseException(String type, String message) {
            super(ErrorCode.AI_PROVIDER_ERROR, message);
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
