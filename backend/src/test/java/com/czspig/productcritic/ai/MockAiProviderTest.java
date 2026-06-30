package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;
import org.junit.jupiter.api.Test;

class MockAiProviderTest {

    private final MockAiProvider provider = new MockAiProvider();

    @Test
    void shouldGenerateDifferentReportsForDifferentIdeas() {
        ReviewReportDto campus = provider.review(request("校园二手交易小程序，帮助学生在毕业季转卖教材和宿舍用品"), "");
        ReviewReportDto resume = provider.review(request("AI 简历优化工具，根据岗位 JD 给求职者修改建议"), "");
        ReviewReportDto pet = provider.review(request("宠物健康管理 App，记录疫苗驱虫和症状日志"), "");

        assertThat(Set.of(
                campus.getBeatScore(),
                resume.getBeatScore(),
                pet.getBeatScore()
        )).hasSize(3);
        assertThat(Set.of(
                campus.getGoDecisionReason(),
                resume.getGoDecisionReason(),
                pet.getGoDecisionReason()
        )).hasSize(3);
        assertThat(campus.getOneLineVerdict()).contains("校园二手交易");
        assertThat(resume.getMvpSuggestions()).anyMatch(item -> item.contains("简历"));
        assertThat(pet.getMinimumBuildVersion().getValidationPlan()).anyMatch(item -> item.contains("宠物"));
        assertThat(campus.getMinimumBuildVersion().getCoreFeatures()).contains("商品发布");
        assertThat(resume.getMinimumBuildVersion().getCoreFeatures()).contains("目标 JD 输入");
        assertThat(pet.getMinimumBuildVersion().getCoreFeatures()).contains("疫苗驱虫提醒");
    }

    @Test
    void shouldInferTargetTypesFromInput() {
        ReviewReportDto idea = provider.review(request("AI 简历优化工具，根据岗位 JD 给求职者修改建议"), "");
        ReviewReportDto client = provider.review(request("甲方需要一个活动报名系统，要求明确报价、排期和验收标准"), "");
        ReviewReportDto mature = provider.review(request("飞书多维表格产品定位复盘，提升企业协作效率"), "");

        assertThat(idea.getReviewTargetType()).isEqualTo("NEW_IDEA");
        assertThat(client.getReviewTargetType()).isEqualTo("CLIENT_REQUIREMENT");
        assertThat(mature.getReviewTargetType()).isEqualTo("MATURE_PRODUCT");
    }

    private CreateReviewRequest request(String content) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setContent(content);
        request.setMode("SHARP_PM");
        request.setRoastLevel(2);
        return request;
    }
}
