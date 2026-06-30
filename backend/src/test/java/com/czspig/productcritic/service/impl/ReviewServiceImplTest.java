package com.czspig.productcritic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.czspig.productcritic.ai.MockAiProvider;
import com.czspig.productcritic.ai.ProductReviewOutputParser;
import com.czspig.productcritic.ai.ProductReviewPromptBuilder;
import com.czspig.productcritic.common.ReviewStatus;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewDetailResponse;
import com.czspig.productcritic.entity.AiCallLogEntity;
import com.czspig.productcritic.entity.ReviewRecordEntity;
import com.czspig.productcritic.mapper.AiCallLogMapper;
import com.czspig.productcritic.mapper.ReviewRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        ReviewServiceImpl service = new ReviewServiceImpl(
                reviewRecordMapper,
                aiCallLogMapper,
                new MockAiProvider(),
                new ProductReviewPromptBuilder(),
                new ProductReviewOutputParser(),
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
