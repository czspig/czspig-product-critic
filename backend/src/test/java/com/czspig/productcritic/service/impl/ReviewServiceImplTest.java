package com.czspig.productcritic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.czspig.productcritic.ai.MockAiProvider;
import com.czspig.productcritic.ai.ProductReviewOutputParser;
import com.czspig.productcritic.ai.ProductReviewPromptBuilder;
import com.czspig.productcritic.ai.ReviewReportRuleEngine;
import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.common.ReviewStatus;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ProjectWorkbenchResponse;
import com.czspig.productcritic.dto.ReviewDetailResponse;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.czspig.productcritic.entity.AiCallLogEntity;
import com.czspig.productcritic.entity.ReviewRecordEntity;
import com.czspig.productcritic.mapper.AiCallLogMapper;
import com.czspig.productcritic.mapper.ReviewRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class ReviewServiceImplTest {

    @Test
    void shouldCreateReviewWithMockProvider() {
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        AiCallLogMapper aiCallLogMapper = mock(AiCallLogMapper.class);
        when(reviewRecordMapper.insert(any(ReviewRecordEntity.class))).thenAnswer(assignReviewId());
        when(reviewRecordMapper.updateById(any(ReviewRecordEntity.class))).thenReturn(1);
        when(reviewRecordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(aiCallLogMapper.insert(any(AiCallLogEntity.class))).thenReturn(1);

        ReviewReportRuleEngine ruleEngine = new ReviewReportRuleEngine();
        ReviewServiceImpl service = new ReviewServiceImpl(
                reviewRecordMapper,
                aiCallLogMapper,
                new MockAiProvider(),
                new ProductReviewPromptBuilder(),
                new ProductReviewOutputParser(ruleEngine),
                ruleEngine,
                new ObjectMapper()
        );

        ReviewDetailResponse response = service.createReview(request(), "test-session");

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getStatus()).isEqualTo(ReviewStatus.SUCCESS.name());
        assertThat(response.getProviderName()).isEqualTo("mock");
        assertThat(response.getModelName()).isEqualTo("mock-product-reviewer-v2");
        assertThat(response.getReport()).isNotNull();
        assertThat(response.getReport().getReviewTargetType()).isEqualTo("NEW_IDEA");
        assertThat(response.getReportJson()).contains("校园二手交易");
        verify(reviewRecordMapper).insert(any(ReviewRecordEntity.class));
        verify(reviewRecordMapper, atLeast(2)).updateById(any(ReviewRecordEntity.class));
        verify(aiCallLogMapper).insert(any(AiCallLogEntity.class));
    }

    @Test
    void shouldAggregateProjectWorkbenchUsingLatestVersion() {
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        AiCallLogMapper aiCallLogMapper = mock(AiCallLogMapper.class);
        ReviewRecordEntity versionOne = projectRecord(
                101L, 1, "V1 校园交易", "CONTINUE", "先验证交易意愿", false);
        ReviewRecordEntity versionTwo = projectRecord(
                102L, 2, "V2 校园教材交易", "PIVOT", "收缩到教材品类", true);
        when(reviewRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(versionTwo, versionOne));

        ProjectWorkbenchResponse response = service(reviewRecordMapper, aiCallLogMapper)
                .getProjectWorkbench("project-1", "test-session");

        assertThat(response.getProjectId()).isEqualTo("project-1");
        assertThat(response.getTitle()).isEqualTo("V2 校园教材交易");
        assertThat(response.getLatestReviewId()).isEqualTo(102L);
        assertThat(response.getLatestVersionNo()).isEqualTo(2);
        assertThat(response.getTotalVersions()).isEqualTo(2);
        assertThat(response.getDecision()).isEqualTo("PIVOT");
        assertThat(response.getProvidedFacts()).hasSize(1);
        assertThat(response.getCriticalHypotheses()).hasSize(1);
        assertThat(response.getUnknownQuestions()).hasSize(1);
        assertThat(response.getValidationPlan()).containsExactly("访谈 5 名毕业生", "发布教材求购表单");
        assertThat(response.getVersions()).extracting(item -> item.getVersionNo()).containsExactly(1, 2);
    }

    @Test
    void shouldReturnStableEmptyCollectionsForMissingOptionalReportFields() {
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        AiCallLogMapper aiCallLogMapper = mock(AiCallLogMapper.class);
        ReviewRecordEntity record = projectRecord(
                101L, 1, "旧版项目", "CONTINUE", "继续验证", false);
        ReviewReportDto report = new ReviewReportDto();
        report.setGoDecision("CONTINUE");
        report.setGoDecisionReason("继续验证");
        report.setKnowledgeBoundary(null);
        report.setMinimumBuildVersion(null);
        record.setReportJson(json(report));
        when(reviewRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(record));

        ProjectWorkbenchResponse response = service(reviewRecordMapper, aiCallLogMapper)
                .getProjectWorkbench("project-1", "test-session");

        assertThat(response.getProvidedFacts()).isEmpty();
        assertThat(response.getInferences()).isEmpty();
        assertThat(response.getCriticalHypotheses()).isEmpty();
        assertThat(response.getUnknownQuestions()).isEmpty();
        assertThat(response.getValidationPlan()).isEmpty();
        assertThat(response.getVersions()).hasSize(1);
        assertThat(response.getVersions().get(0).getCoreFeatures()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenProjectDoesNotExist() {
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        when(reviewRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service(reviewRecordMapper, mock(AiCallLogMapper.class))
                .getProjectWorkbench("missing-project", "test-session"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void shouldHideLegacyProjectOwnedByAnotherSession() {
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewRecordEntity otherSessionRecord = projectRecord(
                42L, 1, "其他会话项目", "CONTINUE", "继续验证", false);
        otherSessionRecord.setSessionId("other-session");
        when(reviewRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(reviewRecordMapper.selectById(42L)).thenReturn(otherSessionRecord);

        assertThatThrownBy(() -> service(reviewRecordMapper, mock(AiCallLogMapper.class))
                .getProjectWorkbench("42", "test-session"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private ReviewServiceImpl service(ReviewRecordMapper reviewRecordMapper, AiCallLogMapper aiCallLogMapper) {
        ReviewReportRuleEngine ruleEngine = new ReviewReportRuleEngine();
        return new ReviewServiceImpl(
                reviewRecordMapper,
                aiCallLogMapper,
                new MockAiProvider(),
                new ProductReviewPromptBuilder(),
                new ProductReviewOutputParser(ruleEngine),
                ruleEngine,
                new ObjectMapper()
        );
    }

    private ReviewRecordEntity projectRecord(
            Long id,
            int versionNo,
            String summary,
            String decision,
            String decisionReason,
            boolean includeWorkbenchDetails) {
        ReviewReportDto report = new ReviewReportDto();
        report.setGoDecision(decision);
        report.setGoDecisionReason(decisionReason);

        ReviewReportDto.MinimumBuildVersion minimum = new ReviewReportDto.MinimumBuildVersion();
        minimum.setGoal("验证教材交易闭环");
        minimum.setCoreFeatures(List.of("发布教材", "联系卖家"));
        minimum.setExcludedFeatures(List.of("支付"));
        minimum.setSuccessMetric("一周内完成 10 次有效联系");
        minimum.setValidationPlan(includeWorkbenchDetails
                ? List.of("访谈 5 名毕业生", "发布教材求购表单")
                : List.of("访谈目标用户"));
        report.setMinimumBuildVersion(minimum);

        if (includeWorkbenchDetails) {
            ReviewReportDto.KnowledgeBoundary boundary = new ReviewReportDto.KnowledgeBoundary();
            ReviewReportDto.ProvidedFact fact = new ReviewReportDto.ProvidedFact();
            fact.setStatement("毕业季存在集中转卖需求");
            fact.setSourceText("用户输入");
            boundary.setProvidedFacts(List.of(fact));

            ReviewReportDto.CriticalHypothesis hypothesis = new ReviewReportDto.CriticalHypothesis();
            hypothesis.setStatement("同校学生愿意线下完成交易");
            hypothesis.setImportance("HIGH");
            boundary.setCriticalHypotheses(List.of(hypothesis));

            ReviewReportDto.UnknownQuestion unknown = new ReviewReportDto.UnknownQuestion();
            unknown.setQuestion("有效供需是否能在一周内匹配");
            unknown.setReason("缺少真实交易数据");
            boundary.setUnknowns(List.of(unknown));
            report.setKnowledgeBoundary(boundary);
        }

        ReviewRecordEntity record = new ReviewRecordEntity();
        record.setId(id);
        record.setSessionId("test-session");
        record.setInputSummary(summary);
        record.setIdeaGroupId("project-1");
        record.setVersionNo(versionNo);
        record.setOneLineVerdict(summary + "结论");
        record.setBeatScore(70 - versionNo);
        record.setPositioningScore(60 + versionNo);
        record.setReportJson(json(report));
        record.setStatus(ReviewStatus.SUCCESS.name());
        record.setCreatedAt(LocalDateTime.of(2026, 7, versionNo, 10, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 7, versionNo, 11, 0));
        return record;
    }

    private String json(ReviewReportDto report) {
        try {
            return new ObjectMapper().writeValueAsString(report);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private CreateReviewRequest request() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setContent("校园二手交易小程序，帮助学生在毕业季转卖教材和宿舍用品");
        request.setMode("SHARP_PM");
        request.setRoastLevel(2);
        return request;
    }

    private Answer<Integer> assignReviewId() {
        return invocation -> {
            ReviewRecordEntity record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        };
    }
}
