package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.czspig.productcritic.dto.ReviewReportDto;
import org.junit.jupiter.api.Test;

class ReviewReportRuleEngineTest {

    private final ReviewReportRuleEngine engine = new ReviewReportRuleEngine();

    @Test
    void shouldNormalizeTrustedStructure() {
        ReviewReportDto report = baseReport();

        engine.normalizeTrustedReview(report);
        engine.audit(report);

        assertThat(report.getDimensionScores()).hasSize(6);
        assertThat(report.getOverallAssessment()).isNotNull();
        assertThat(report.getKnowledgeBoundary()).isNotNull();
        assertThat(report.getQualityAudit().getChecks()).extracting(ReviewReportDto.QualityCheck::getKey)
                .contains("COMPLETENESS", "SCORE_EVIDENCE", "FACT_BOUNDARY", "STRUCTURE_VALIDITY");
    }

    @Test
    void shouldWarnWhenHighRiskStillProceed() {
        ReviewReportDto report = baseReport();
        engine.normalizeTrustedReview(report);
        report.getOverallAssessment().setDecision("PROCEED");
        report.getDimensionScores().stream()
                .filter(item -> "riskLevel".equals(item.getDimensionKey()))
                .findFirst()
                .orElseThrow()
                .setScore(90);

        ReviewReportDto.QualityAudit audit = engine.audit(report);

        assertThat(audit.getOverallStatus()).isEqualTo("WARNING");
        assertThat(audit.getIssues()).extracting(ReviewReportDto.QualityIssue::getType)
                .contains("DECISION_SCORE_CONFLICT");
    }

    private ReviewReportDto baseReport() {
        ReviewReportDto report = new ReviewReportDto();
        report.setOneLineVerdict("可以继续，但先验证真实需求。");
        report.setReviewTargetType("NEW_IDEA");
        report.setGoDecision("CONTINUE");
        report.setGoDecisionReason("目标用户和痛点初步存在，但商业化仍需验证。");
        report.setBeatScore(55);
        report.setPositioningScore(68);
        report.setPainPointAnalysis("目标用户是大学生，痛点是小组作业分工不透明。");
        report.setFakeDemandRisks(List.of("可能只是课程临时需求。"));
        report.setFeatureRedundancyCheck(List.of("首版不做复杂权限。"));
        report.setColdStartProblems(List.of("需要先从一个班级启动。"));
        report.setMvpSuggestions(List.of("访谈 5 个目标用户并手动验证任务分工流程。"));
        report.setDeveloperPrompt("实现一个最小可用版本。");

        ReviewReportDto.MinimumBuildVersion minimum = new ReviewReportDto.MinimumBuildVersion();
        minimum.setGoal("验证学生是否愿意持续记录分工。");
        minimum.setCoreFeatures(List.of("创建项目", "分配任务"));
        minimum.setExcludedFeatures(List.of("复杂统计"));
        minimum.setSuccessMetric("7 天内至少 3 个小组完成一次任务流转。");
        minimum.setValidationPlan(List.of("访谈 5 个学生", "发布一个表单原型测试"));
        report.setMinimumBuildVersion(minimum);
        return report;
    }
}
