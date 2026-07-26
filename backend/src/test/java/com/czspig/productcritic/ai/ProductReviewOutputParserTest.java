package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.czspig.productcritic.dto.ReviewReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProductReviewOutputParserTest {

    private final ProductReviewOutputParser parser = new ProductReviewOutputParser(new ReviewReportRuleEngine());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseCompleteJson() {
        ReviewReportDto report = parser.parseAndValidate(completeJson(), objectMapper);

        assertThat(report.getReviewTargetType()).isEqualTo("NEW_IDEA");
        assertThat(report.getMinimumBuildVersion().getValidationPlan()).contains("访谈 5 个用户");
    }

    @Test
    void shouldParseMarkdownWrappedJson() {
        ReviewReportDto report = parser.parseAndValidate("```json\n" + completeJson() + "\n```", objectMapper);

        assertThat(report.getGoDecision()).isEqualTo("CONTINUE");
    }

    @Test
    void shouldStrictlyParsePureJson() {
        ReviewReportDto report = parser.parseStrictAndValidate(strictJson("68", "medium"), objectMapper);

        assertThat(report.getDimensionScores()).hasSize(6);
        assertThat(report.getDimensionScores().get(0).getScore()).isEqualTo(68);
        assertThat(report.getDimensionScores().get(0).getConfidence()).isEqualTo("MEDIUM");
    }

    @Test
    void shouldStrictlyParseJsonInsideMarkdownFence() {
        ReviewReportDto report = parser.parseStrictAndValidate("```json\n" + strictJson("68", "HIGH") + "\n```", objectMapper);

        assertThat(report.getKnowledgeBoundary().getProvidedFacts()).isNotEmpty();
    }

    @Test
    void shouldStrictlyParseJsonWithTextAroundIt() {
        ReviewReportDto report = parser.parseStrictAndValidate("以下是结果：\n" + strictJson("68", "HIGH") + "\n希望有帮助", objectMapper);

        assertThat(report.getOverallAssessment().getDecision()).isEqualTo("VALIDATE_FIRST");
    }

    @Test
    void shouldHandleBomAndBracesInsideString() {
        ReviewReportDto report = parser.parseStrictAndValidate("\uFEFF" + strictJson("68", "HIGH")
                .replace("可以继续，但要先收缩。", "可以继续，但用户原话里有 {小组任务} 这样的描述。"), objectMapper);

        assertThat(report.getOneLineVerdict()).contains("{小组任务}");
    }

    @Test
    void shouldNormalizeNullArraysAllowedBySchema() {
        ReviewReportDto report = parser.parseStrictAndValidate(strictJson("68", "HIGH")
                .replace("\"evidence\": [\"用户输入提到任务负责人不明确\"],", "\"evidence\": null,"), objectMapper);

        assertThat(report.getDimensionScores().get(0).getEvidence()).isNotEmpty();
    }

    @Test
    void shouldRejectMissingCoreObjectsInStrictMode() {
        assertThatThrownBy(() -> parser.parseStrictAndValidate(completeJson(), objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("overallAssessment");
    }

    @Test
    void shouldRejectMissingDimensionInStrictMode() {
        String missingRisk = strictJson("68", "HIGH")
                .replaceAll("(?s),\\s*\\{\\s*\"dimensionKey\": \"riskLevel\".*?\"changeReason\": \"基于当前输入判断\"\\s*\\}", "");
        assertThatThrownBy(() -> parser.parseStrictAndValidate(missingRisk, objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("dimensionScores 数量");
    }

    @Test
    void shouldRejectDuplicateDimensionInStrictMode() {
        assertThatThrownBy(() -> parser.parseStrictAndValidate(strictJson("68", "HIGH")
                .replace("\"dimensionKey\": \"riskLevel\"", "\"dimensionKey\": \"painPoint\""), objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("重复维度");
    }

    @Test
    void shouldRejectInvalidDirectionInStrictMode() {
        assertThatThrownBy(() -> parser.parseStrictAndValidate(strictJson("68", "HIGH")
                .replaceFirst("\"scoreDirection\": \"HIGHER_IS_WORSE\"", "\"scoreDirection\": \"HIGHER_IS_BETTER\""), objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("scoreDirection");
    }

    @Test
    void shouldRejectInvalidConfidenceInStrictMode() {
        assertThatThrownBy(() -> parser.parseStrictAndValidate(strictJson("68", "中等"), objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("confidence");
    }

    @Test
    void shouldRejectTruncatedJsonInStrictMode() {
        assertThatThrownBy(() -> parser.parseStrictAndValidate(strictJson("68", "HIGH").substring(0, 200), objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("截断");
    }

    @Test
    void shouldRejectTopLevelArrayInStrictMode() {
        assertThatThrownBy(() -> parser.parseStrictAndValidate("[{\"oneLineVerdict\":\"x\"}]", objectMapper))
                .isInstanceOf(ProductReviewOutputParser.OutputParseException.class)
                .hasMessageContaining("顶层是数组");
    }

    @Test
    void shouldFillMissingFields() {
        ReviewReportDto report = parser.parseAndValidate("""
                {
                  "oneLineVerdict": "输入太宽，需要先收缩。",
                  "beatScore": 88
                }
                """, objectMapper);

        assertThat(report.getReviewTargetType()).isEqualTo("UNCLEAR");
        assertThat(report.getGoDecision()).isEqualTo("PAUSE");
        assertThat(report.getPositioningScore()).isEqualTo(35);
        assertThat(report.getMinimumBuildVersion().getSuccessMetric()).isNotBlank();
        assertThat(report.getDeveloperPrompt()).isNotBlank();
    }

    private String completeJson() {
        return """
                {
                  "oneLineVerdict": "可以继续，但要先收缩。",
                  "reviewTargetType": "NEW_IDEA",
                  "goDecision": "CONTINUE",
                  "goDecisionReason": "目标用户初步明确，最大风险是冷启动。",
                  "beatScore": 62,
                  "positioningScore": 70,
                  "painPointAnalysis": "目标用户是学生，痛点是交易信任和效率。",
                  "fakeDemandRisks": ["可能只是毕业季短期需求"],
                  "featureRedundancyCheck": ["第一版不做支付担保"],
                  "coldStartProblems": ["先从一个校区启动"],
                  "mvpSuggestions": ["只做发布和联系闭环"],
                  "minimumBuildVersion": {
                    "goal": "验证同校交易是否成立",
                    "coreFeatures": ["发布商品"],
                    "excludedFeatures": ["支付担保"],
                    "successMetric": "完成 30 单交易",
                    "validationPlan": ["访谈 5 个用户"]
                  },
                  "developerPrompt": "请实现最小交易闭环。"
                }
                """;
    }

    private String strictJson(String scoreValue, String confidence) {
        return """
                {
                  "oneLineVerdict": "可以继续，但要先收缩。",
                  "reviewTargetType": "NEW_IDEA",
                  "goDecision": "PIVOT",
                  "goDecisionReason": "目标用户初步明确，最大风险是迁移成本。",
                  "beatScore": 62,
                  "positioningScore": 70,
                  "overallAssessment": {
                    "overallScore": 66,
                    "decision": "VALIDATE_FIRST",
                    "strongestDimension": "用户痛点",
                    "weakestDimension": "商业模式",
                    "mostUncertainDimension": "增长潜力",
                    "priorityQuestion": "学生是否愿意从微信群迁移？",
                    "overallReasoning": "需求存在，但需要验证小组真实使用频率。"
                  },
                  "dimensionScores": [
                %s
                %s
                %s
                %s
                %s
                %s
                  ],
                  "knowledgeBoundary": {
                    "providedFacts": [{"statement": "用户输入提到课程小组作业", "sourceText": "面向大学生课程小组作业"}],
                    "inferences": [{"statement": "迁移成本可能较高", "basis": "微信群是现有替代方案", "confidence": "medium"}],
                    "criticalHypotheses": [{"statement": "学生愿意额外打开工具", "importance": "HIGH", "impactIfFalse": "产品无法留存", "relatedDimensions": ["用户痛点"]}],
                    "unknowns": [{"question": "谁负责维护任务？", "reason": "输入未说明负责人机制"}]
                  },
                  "qualityAudit": {
                    "overallStatus": "warning",
                    "checks": [{"key": "COMPLETENESS", "name": "报告完整性", "status": "PASS", "message": "结构完整", "relatedPaths": []}],
                    "issues": [],
                    "summary": "结构基本可用"
                  },
                  "painPointAnalysis": "学生小组作业分工不清晰。",
                  "fakeDemandRisks": ["可能只是少数课程需要"],
                  "featureRedundancyCheck": ["首版不做复杂权限"],
                  "coldStartProblems": ["先邀请 10 个小组测试"],
                  "mvpSuggestions": ["用表单和提醒验证闭环"],
                  "minimumBuildVersion": {
                    "goal": "验证学生是否愿意持续记录分工",
                    "coreFeatures": ["创建小组", "分配任务"],
                    "excludedFeatures": ["复杂统计"],
                    "successMetric": "10 个小组中至少 6 个完成一次任务流转",
                    "validationPlan": ["邀请 10 个小组免费测试"]
                  },
                  "developerPrompt": "实现课程小组任务协作 MVP。"
                }
                """.formatted(
                dimension("painPoint", "用户痛点", "HIGHER_IS_BETTER", confidence).replace("\"score\": 68", "\"score\": " + quoteIfNeeded(scoreValue)),
                dimension("marketSpace", "市场空间", "HIGHER_IS_BETTER", "HIGH"),
                dimension("businessModel", "商业模式", "HIGHER_IS_BETTER", "MEDIUM"),
                dimension("implementationDifficulty", "实现难度", "HIGHER_IS_WORSE", "MEDIUM"),
                dimension("growthPotential", "增长潜力", "HIGHER_IS_BETTER", "LOW"),
                dimension("riskLevel", "风险等级", "HIGHER_IS_WORSE", "MEDIUM").replaceAll(",\\s*$", "")
        );
    }

    private String dimension(String key, String name, String direction, String confidence) {
        return """
                    {
                      "dimensionKey": "%s",
                      "dimensionName": "%s",
                      "score": 68,
                      "confidence": "%s",
                      "scoreDirection": "%s",
                      "summary": "%s 需要继续验证。",
                      "evidence": ["用户输入提到任务负责人不明确"],
                      "deductions": ["未证明迁移意愿"],
                      "missingInformation": ["真实使用频率"],
                      "improvementActions": ["访谈 10 个课程小组"],
                      "changeReason": "基于当前输入判断"
                    },
                """.formatted(key, name, confidence, direction, name);
    }

    private String quoteIfNeeded(String value) {
        return value.matches("\\d+") ? "\"" + value + "\"" : value;
    }
}
