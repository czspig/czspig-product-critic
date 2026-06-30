package com.czspig.productcritic.controller;

import com.czspig.productcritic.common.ApiResponse;
import com.czspig.productcritic.common.PageResponse;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewDetailResponse;
import com.czspig.productcritic.dto.ReviewGroupResponse;
import com.czspig.productcritic.dto.ReviewListItemResponse;
import com.czspig.productcritic.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final String anonymousSessionId;

    public ReviewController(
            ReviewService reviewService,
            @Value("${app.anonymous-session-id:anonymous-dev-session}") String anonymousSessionId) {
        this.reviewService = reviewService;
        this.anonymousSessionId = anonymousSessionId;
    }

    @PostMapping
    public ApiResponse<ReviewDetailResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ApiResponse.ok(reviewService.createReview(request, resolveSessionId(sessionId)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewListItemResponse>> listReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ApiResponse.ok(reviewService.listReviews(page, pageSize, resolveSessionId(sessionId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewDetailResponse> getReview(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ApiResponse.ok(reviewService.getReview(id, resolveSessionId(sessionId)));
    }

    @GetMapping("/group/{ideaGroupId}")
    public ApiResponse<ReviewGroupResponse> getReviewGroup(
            @PathVariable String ideaGroupId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ApiResponse.ok(reviewService.getReviewGroup(ideaGroupId, resolveSessionId(sessionId)));
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return anonymousSessionId;
        }
        return sessionId.trim();
    }
}
