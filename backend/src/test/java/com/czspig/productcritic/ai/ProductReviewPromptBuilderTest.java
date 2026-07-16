package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.czspig.productcritic.dto.CreateReviewRequest;
import org.junit.jupiter.api.Test;

class ProductReviewPromptBuilderTest {

    private final ProductReviewPromptBuilder builder = new ProductReviewPromptBuilder();

    @Test
    void shouldIncludeRoastLevelRuleInPrompt() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setContent("AI resume JD tool for job seekers");
        request.setMode("SHARP_PM");
        request.setRoastLevel(3);

        String prompt = builder.build(request);

        assertThat(prompt).contains("\u6bd2\u820c\u5f3a\u5ea6\uff1a3");
        assertThat(prompt).contains("\u53ef\u4ee5\u66f4\u6709\u950b\u8292");
        assertThat(prompt).contains("<user_input>");
    }

    @Test
    void shouldDefaultMissingRoastLevelToSharpMode() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setContent("AI resume JD tool for job seekers");
        request.setMode("SHARP_PM");

        String prompt = builder.build(request);

        assertThat(prompt).contains("\u6bd2\u820c\u5f3a\u5ea6\uff1a2");
        assertThat(prompt).contains("\u76f4\u63a5\u6307\u51fa\u95ee\u9898");
    }
}
