package com.czspig.productcritic.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.czspig.productcritic.dto.ReviewReportDto;
import org.springframework.stereotype.Component;

@Component
public class ReviewReportRuleEngine {

    private static final List<DimensionMeta> DIMENSIONS = List.of(
            new DimensionMeta("painPoint", "用户痛点", "HIGHER_IS_BETTER"),
            new DimensionMeta("marketSpace", "市场空间", "HIGHER_IS_BETTER"),
            new DimensionMeta("businessModel", "商业模式", "HIGHER_IS_BETTER"),
            new DimensionMeta("implementationDifficulty", "实现难度", "HIGHER_IS_WORSE"),
            new DimensionMeta("growthPotential", "增长潜力", "HIGHER_IS_BETTER"),
            new DimensionMeta("riskLevel", "风险等级", "HIGHER_IS_WORSE")
    );
    private static final Set<String> CONFIDENCE = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> ASSESSMENT_DECISIONS = Set.of("PROCEED", "VALIDATE_FIRST", "NARROW_SCOPE", "PAUSE");
    private static final Set<String> STATUS = Set.of("PASS", "WARNING", "FAIL");
    private static final Map<String, Integer> IMPORTANCE_ORDER = Map.of("HIGH", 0, "MEDIUM", 1, "LOW", 2);

    public void normalizeTrustedReview(ReviewReportDto report) {
        if (report == null) {
            return;
        }
        normalizeDimensionScores(report);
        normalizeKnowledgeBoundary(report);
        normalizeOverallAssessment(report);
        normalizeQualityAudit(report);
    }

    public ReviewReportDto.QualityAudit audit(ReviewReportDto report) {
        ReviewReportDto.QualityAudit audit = report.getQualityAudit();
        if (audit == null) {
            audit = new ReviewReportDto.QualityAudit();
        }
        List<ReviewReportDto.QualityCheck> checks = new ArrayList<>();
        List<ReviewReportDto.QualityIssue> issues = new ArrayList<>();

        boolean complete = hasText(report.getOneLineVerdict())
                && hasText(report.getGoDecisionReason())
                && hasText(report.getPainPointAnalysis())
                && report.getMinimumBuildVersion() != null
                && notEmpty(report.getDimensionScores())
                && report.getKnowledgeBoundary() != null;
        checks.add(check("COMPLETENESS", "报告完整性", complete ? "PASS" : "FAIL",
                complete ? "核心评审字段和可信评审结构均已生成" : "报告缺少核心字段、评分证据链或事实边界结构"));
        if (!complete) {
            issues.add(issue("HIGH", "MISSING_TRUST_STRUCTURE", "报告缺少可信评审结构", "重新生成报告或触发 JSON 修复"));
        }

        long invalidDimensions = report.getDimensionScores() == null ? 6 : report.getDimensionScores().stream()
                .filter(item -> !validScore(item.getScore())
                        || !CONFIDENCE.contains(safeUpper(item.getConfidence()))
                        || !validDirection(item.getDimensionKey(), item.getScoreDirection())
                        || !notEmpty(item.getEvidence()))
                .count();
        checks.add(check("SCORE_EVIDENCE", "评分证据链", invalidDimensions == 0 ? "PASS" : "FAIL",
                invalidDimensions == 0 ? "每个维度都有分数、置信度、方向和证据" : invalidDimensions + " 个维度缺少必要评分依据或方向"));

        ReviewReportDto.KnowledgeBoundary boundary = report.getKnowledgeBoundary();
        boolean factBoundaryOk = boundary != null
                && safeList(boundary.getProvidedFacts()).stream().allMatch(fact -> hasText(fact.getStatement()) && hasText(fact.getSourceText()))
                && safeList(boundary.getCriticalHypotheses()).stream().noneMatch(item -> !Set.of("HIGH", "MEDIUM", "LOW").contains(safeUpper(item.getImportance())));
        checks.add(check("FACT_BOUNDARY", "事实边界", factBoundaryOk ? "PASS" : "WARNING",
                factBoundaryOk ? "事实均带有原始输入引用，假设重要程度合法" : "存在缺少来源的事实或非法假设优先级"));
        if (!factBoundaryOk) {
            issues.add(issue("MEDIUM", "FACT_BOUNDARY_WEAK", "事实与推测边界需要复核", "只把用户原文可定位的信息放入事实列表"));
        }

        String consistencyStatus = decisionConsistencyStatus(report);
        checks.add(check("DECISION_CONSISTENCY", "评分与结论一致性", consistencyStatus,
                "根据总分、风险分和总体建议完成一致性检查"));
        if (!"PASS".equals(consistencyStatus)) {
            issues.add(issue("MEDIUM", "DECISION_SCORE_CONFLICT", "评分和总体建议存在潜在冲突", "确认是否存在合规风险或其他一票否决理由"));
        }

        boolean actionable = safeList(report.getMvpSuggestions()).stream().anyMatch(this::looksActionable)
                && report.getMinimumBuildVersion() != null
                && safeList(report.getMinimumBuildVersion().getValidationPlan()).stream().anyMatch(this::looksActionable);
        checks.add(check("ACTIONABILITY", "建议可执行性", actionable ? "PASS" : "WARNING",
                actionable ? "MVP 建议和验证计划包含可执行动作" : "建议仍偏泛，需要补充对象、动作或验收标准"));

        boolean mvpScopeOk = report.getMinimumBuildVersion() != null
                && safeList(report.getMinimumBuildVersion().getCoreFeatures()).size() <= 6;
        checks.add(check("MVP_SCOPE", "MVP 范围合理性", mvpScopeOk ? "PASS" : "WARNING",
                mvpScopeOk ? "最小版本功能数量可控" : "第一版功能偏多，建议继续收缩"));

        checks.add(check("ROAST_LEVEL", "毒舌程度匹配", "PASS",
                "毒舌程度只影响表达风格，不改变事实边界和评分逻辑"));

        String structureStatus = invalidDimensions == 0 ? "PASS" : "FAIL";
        checks.add(check("STRUCTURE_VALIDITY", "输出结构合法性", structureStatus,
                structureStatus.equals("PASS") ? "枚举、分数、数组和方向字段合法" : "存在非法枚举、分数越界或缺少方向字段"));

        audit.setChecks(checks);
        audit.setIssues(issues);
        audit.setOverallStatus(aggregateStatus(checks));
        audit.setSummary(summaryFor(audit.getOverallStatus(), issues.size()));
        report.setQualityAudit(audit);
        return audit;
    }

    private void normalizeDimensionScores(ReviewReportDto report) {
        List<ReviewReportDto.DimensionScore> source = new ArrayList<>(safeList(report.getDimensionScores()));
        List<ReviewReportDto.DimensionScore> normalized = new ArrayList<>();
        for (DimensionMeta meta : DIMENSIONS) {
            ReviewReportDto.DimensionScore item = source.stream()
                    .filter(candidate -> meta.key().equals(candidate.getDimensionKey()))
                    .findFirst()
                    .orElseGet(() -> legacyDimension(report, meta));
            item.setDimensionKey(meta.key());
            item.setDimensionName(blankToDefault(item.getDimensionName(), meta.name()));
            item.setScore(clamp(item.getScore(), defaultScore(report, meta.key())));
            item.setConfidence(normalizeEnum(item.getConfidence(), CONFIDENCE, "LOW"));
            item.setScoreDirection(validDirection(meta.key(), item.getScoreDirection()) ? item.getScoreDirection() : meta.direction());
            item.setSummary(blankToDefault(item.getSummary(), "该维度需要结合证据继续验证。"));
            item.setEvidence(cleanList(item.getEvidence(), "当前评分依据不足，需要补充可定位证据。"));
            item.setDeductions(cleanList(item.getDeductions(), "缺少足够信息支撑更高置信度判断。"));
            item.setMissingInformation(cleanList(item.getMissingInformation(), "目标用户、替代方案或验证数据仍需补充。"));
            item.setImprovementActions(cleanList(item.getImprovementActions(), "用 5-10 位目标用户访谈或手工实验验证该维度。"));
            item.setChangeReason(blankToDefault(item.getChangeReason(), "本版基于当前输入和报告内容形成判断。"));
            normalized.add(item);
        }
        report.setDimensionScores(normalized);
    }

    private ReviewReportDto.DimensionScore legacyDimension(ReviewReportDto report, DimensionMeta meta) {
        ReviewReportDto.DimensionScore item = new ReviewReportDto.DimensionScore();
        item.setDimensionKey(meta.key());
        item.setDimensionName(meta.name());
        item.setScore(defaultScore(report, meta.key()));
        item.setConfidence("LOW");
        item.setScoreDirection(meta.direction());
        item.setSummary("模型未提供完整证据链，系统按已有报告字段生成最低限度校验项。");
        item.setEvidence(List.of("来自旧版或不完整结构的报告字段，证据链不完整。"));
        item.setDeductions(List.of("缺少该维度的独立证据、扣分项和验证动作。"));
        item.setMissingInformation(List.of("需要重新生成可信评审报告以获得完整依据。"));
        item.setImprovementActions(List.of("使用相同 idea 重新提交评审，补齐目标用户、场景、差异化和商业模式。"));
        return item;
    }

    private int defaultScore(ReviewReportDto report, String key) {
        int positioning = report.getPositioningScore() == null ? 45 : report.getPositioningScore();
        int risk = report.getBeatScore() == null ? 70 : report.getBeatScore();
        return switch (key) {
            case "painPoint" -> positioning;
            case "marketSpace" -> clamp(positioning - 8, 50);
            case "businessModel" -> clamp(positioning - 18, 40);
            case "implementationDifficulty" -> clamp(100 - positioning + 20, 60);
            case "growthPotential" -> clamp(positioning - 6, 45);
            case "riskLevel" -> risk;
            default -> 50;
        };
    }

    private void normalizeKnowledgeBoundary(ReviewReportDto report) {
        ReviewReportDto.KnowledgeBoundary boundary = report.getKnowledgeBoundary();
        if (boundary == null) {
            boundary = new ReviewReportDto.KnowledgeBoundary();
        }
        boundary.setProvidedFacts(safeList(boundary.getProvidedFacts()).stream()
                .filter(fact -> hasText(fact.getStatement()) && hasText(fact.getSourceText()))
                .limit(5)
                .toList());
        boundary.setInferences(safeList(boundary.getInferences()).stream()
                .peek(item -> item.setConfidence(normalizeEnum(item.getConfidence(), CONFIDENCE, "LOW")))
                .filter(item -> hasText(item.getStatement()))
                .limit(5)
                .toList());
        boundary.setCriticalHypotheses(safeList(boundary.getCriticalHypotheses()).stream()
                .peek(item -> {
                    item.setImportance(normalizeEnum(item.getImportance(), Set.of("HIGH", "MEDIUM", "LOW"), "MEDIUM"));
                    item.setRelatedDimensions(cleanList(item.getRelatedDimensions(), "总体判断"));
                })
                .filter(item -> hasText(item.getStatement()))
                .sorted(Comparator.comparingInt(item -> IMPORTANCE_ORDER.getOrDefault(item.getImportance(), 9)))
                .limit(5)
                .toList());
        boundary.setUnknowns(safeList(boundary.getUnknowns()).stream()
                .filter(item -> hasText(item.getQuestion()))
                .limit(5)
                .toList());
        report.setKnowledgeBoundary(boundary);
    }

    private void normalizeOverallAssessment(ReviewReportDto report) {
        ReviewReportDto.OverallAssessment overall = report.getOverallAssessment();
        if (overall == null) {
            overall = new ReviewReportDto.OverallAssessment();
        }
        overall.setOverallScore(clamp(overall.getOverallScore(), deriveOverallScore(report)));
        overall.setDecision(normalizeEnum(overall.getDecision(), ASSESSMENT_DECISIONS, mapLegacyDecision(report.getGoDecision())));
        ReviewReportDto.DimensionScore strongest = bestPositiveDimension(report.getDimensionScores());
        ReviewReportDto.DimensionScore weakest = weakestDimension(report.getDimensionScores());
        ReviewReportDto.DimensionScore uncertain = report.getDimensionScores().stream()
                .filter(item -> "LOW".equals(item.getConfidence()))
                .findFirst()
                .orElse(weakest);
        overall.setStrongestDimension(blankToDefault(overall.getStrongestDimension(), strongest.getDimensionName()));
        overall.setWeakestDimension(blankToDefault(overall.getWeakestDimension(), weakest.getDimensionName()));
        overall.setMostUncertainDimension(blankToDefault(overall.getMostUncertainDimension(), uncertain.getDimensionName()));
        overall.setPriorityQuestion(blankToDefault(overall.getPriorityQuestion(), firstUnknownQuestion(report)));
        overall.setOverallReasoning(blankToDefault(overall.getOverallReasoning(), report.getGoDecisionReason()));
        report.setOverallAssessment(overall);
    }

    private void normalizeQualityAudit(ReviewReportDto report) {
        ReviewReportDto.QualityAudit audit = report.getQualityAudit();
        if (audit == null) {
            audit = new ReviewReportDto.QualityAudit();
        }
        audit.setChecks(safeList(audit.getChecks()).stream()
                .peek(item -> item.setStatus(normalizeEnum(item.getStatus(), STATUS, "WARNING")))
                .filter(item -> hasText(item.getKey()) && hasText(item.getName()))
                .toList());
        audit.setIssues(safeList(audit.getIssues()).stream()
                .filter(item -> hasText(item.getMessage()))
                .toList());
        audit.setOverallStatus(normalizeEnum(audit.getOverallStatus(), STATUS, "WARNING"));
        audit.setSummary(blankToDefault(audit.getSummary(), "报告质量检查将由后端规则重新聚合。"));
        report.setQualityAudit(audit);
    }

    private String decisionConsistencyStatus(ReviewReportDto report) {
        ReviewReportDto.OverallAssessment overall = report.getOverallAssessment();
        if (overall == null || overall.getOverallScore() == null || overall.getDecision() == null) {
            return "WARNING";
        }
        int risk = report.getDimensionScores().stream()
                .filter(item -> "riskLevel".equals(item.getDimensionKey()))
                .findFirst()
                .map(ReviewReportDto.DimensionScore::getScore)
                .orElse(report.getBeatScore() == null ? 50 : report.getBeatScore());
        if (overall.getOverallScore() >= 80 && "PAUSE".equals(overall.getDecision()) && risk < 80) {
            return "WARNING";
        }
        if (risk >= 85 && "PROCEED".equals(overall.getDecision())) {
            return "WARNING";
        }
        return "PASS";
    }

    private ReviewReportDto.DimensionScore bestPositiveDimension(List<ReviewReportDto.DimensionScore> dimensions) {
        return dimensions.stream()
                .filter(item -> "HIGHER_IS_BETTER".equals(item.getScoreDirection()))
                .max(Comparator.comparingInt(item -> item.getScore() == null ? 0 : item.getScore()))
                .orElse(dimensions.get(0));
    }

    private ReviewReportDto.DimensionScore weakestDimension(List<ReviewReportDto.DimensionScore> dimensions) {
        return dimensions.stream()
                .min(Comparator.comparingInt(item -> {
                    int score = item.getScore() == null ? 50 : item.getScore();
                    return "HIGHER_IS_WORSE".equals(item.getScoreDirection()) ? 100 - score : score;
                }))
                .orElse(dimensions.get(0));
    }

    private int deriveOverallScore(ReviewReportDto report) {
        int positioning = report.getPositioningScore() == null ? 45 : report.getPositioningScore();
        int risk = report.getBeatScore() == null ? 70 : report.getBeatScore();
        return clamp(Math.round(positioning * 0.7f + (100 - risk) * 0.3f), 50);
    }

    private String firstUnknownQuestion(ReviewReportDto report) {
        if (report.getKnowledgeBoundary() != null && notEmpty(report.getKnowledgeBoundary().getUnknowns())) {
            return report.getKnowledgeBoundary().getUnknowns().get(0).getQuestion();
        }
        return "哪一个关键假设最值得用最小实验优先验证？";
    }

    private String mapLegacyDecision(String decision) {
        return switch (decision == null ? "" : decision) {
            case "CONTINUE" -> "PROCEED";
            case "PIVOT" -> "NARROW_SCOPE";
            default -> "VALIDATE_FIRST";
        };
    }

    private boolean looksActionable(String value) {
        if (!hasText(value)) {
            return false;
        }
        return value.matches(".*(访谈|验证|记录|收集|测试|确认|导入|观察|统计|限定|招募|交付|签字|发布|连续|至少|\\d+).*");
    }

    private ReviewReportDto.QualityCheck check(String key, String name, String status, String message) {
        ReviewReportDto.QualityCheck check = new ReviewReportDto.QualityCheck();
        check.setKey(key);
        check.setName(name);
        check.setStatus(status);
        check.setMessage(message);
        return check;
    }

    private ReviewReportDto.QualityIssue issue(String severity, String type, String message, String suggestion) {
        ReviewReportDto.QualityIssue issue = new ReviewReportDto.QualityIssue();
        issue.setSeverity(severity);
        issue.setType(type);
        issue.setMessage(message);
        issue.setSuggestion(suggestion);
        return issue;
    }

    private String aggregateStatus(List<ReviewReportDto.QualityCheck> checks) {
        if (checks.stream().anyMatch(item -> "FAIL".equals(item.getStatus()))) {
            return "FAIL";
        }
        if (checks.stream().anyMatch(item -> "WARNING".equals(item.getStatus()))) {
            return "WARNING";
        }
        return "PASS";
    }

    private String summaryFor(String status, int issueCount) {
        return switch (status) {
            case "PASS" -> "报告结构完整，证据链、事实边界和建议可执行性通过基础检查。";
            case "FAIL" -> "报告存在关键结构缺失或非法字段，需要重新生成或修复后再展示。";
            default -> "报告可用，但存在 " + issueCount + " 项需要注意的可信度问题。";
        };
    }

    private boolean validDirection(String key, String direction) {
        return DIMENSIONS.stream().anyMatch(meta -> meta.key().equals(key) && meta.direction().equals(direction));
    }

    private boolean validScore(Integer value) {
        return value != null && value >= 0 && value <= 100;
    }

    private boolean notEmpty(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private List<String> cleanList(List<String> values, String fallback) {
        List<String> cleaned = safeList(values).stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
        return cleaned.isEmpty() ? List.of(fallback) : cleaned;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToDefault(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String normalizeEnum(String value, Set<String> allowed, String fallback) {
        String normalized = safeUpper(value);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private int clamp(Integer value, int fallback) {
        int safe = value == null ? fallback : value;
        return Math.max(0, Math.min(100, safe));
    }

    private record DimensionMeta(String key, String name, String direction) {
    }
}
