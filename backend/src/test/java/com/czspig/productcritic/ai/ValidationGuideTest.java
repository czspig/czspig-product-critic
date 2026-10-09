package com.czspig.productcritic.ai;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ValidationGuideTest {
    @Test void shouldPreserveGuideAndRejectIncompleteExperiments() throws Exception {
        var request = new CreateReviewRequest(); request.setContent("校园社团报名工具，负责人用表格统计，学生提交报名。我们尚未访谈或验证。"); request.setMode("SHARP_PM"); request.setRoastLevel(2);
        var report = new MockAiProvider().review(request, "");
        var mapper = new ObjectMapper();
        var parser = new ProductReviewOutputParser(new ReviewReportRuleEngine());
        var decoded = parser.parseAndValidate(mapper.writeValueAsString(report), mapper);
        assertThat(decoded.getValidationGuide().informationInsufficient()).isTrue();
        assertThat(decoded.getValidationGuide().risks()).hasSize(3).allSatisfy(r -> {
            assertThat(r.evidenceType()).isEqualTo("HYPOTHESIS"); assertThat(r.steps()).isNotEmpty();
            assertThat(r.thresholdReason()).isNotBlank(); assertThat(r.evidenceToCollect()).isNotEmpty();
        });
        var guide = decoded.getValidationGuide();
        decoded.setValidationGuide(new ReviewReportDto.ValidationGuide(true, guide.decisionReason(), java.util.List.of(), guide.continueWhen(), guide.adjustWhen(), guide.pauseWhen()));
        assertThatThrownBy(() -> parser.validate(decoded)).hasMessageContaining("三个核心风险");
        decoded.setValidationGuide(null);
        assertThatCode(() -> parser.validate(decoded)).doesNotThrowAnyException();
    }
}
