package com.czspig.productcritic.service;

import com.czspig.productcritic.common.PageResponse;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewDetailResponse;
import com.czspig.productcritic.dto.ReviewListItemResponse;

public interface ReviewService {

    ReviewDetailResponse createReview(CreateReviewRequest request, String sessionId);

    PageResponse<ReviewListItemResponse> listReviews(int page, int pageSize, String sessionId);

    ReviewDetailResponse getReview(Long id, String sessionId);
}
