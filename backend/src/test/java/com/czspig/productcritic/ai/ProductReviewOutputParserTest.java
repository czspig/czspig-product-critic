package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.czspig.productcritic.dto.ReviewReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProductReviewOutputParserTest {

    private final ProductReviewOutputParser parser = new ProductReviewOutputParser();
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
}
