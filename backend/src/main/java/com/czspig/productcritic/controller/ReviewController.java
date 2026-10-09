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

    @org.springframework.beans.factory.annotation.Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @Value("${app.auth.anonymous-quota:3}") private int quota;
    @Value("${app.auth.daily-review-limit:200}") private int dailyLimit;
    private final ReviewService reviewService;
    private final String anonymousSessionId;

    public ReviewController(
            ReviewService reviewService,
            @Value("${app.anonymous-session-id:anonymous-dev-session}") String anonymousSessionId) {
        this.reviewService = reviewService;
        this.anonymousSessionId = anonymousSessionId;
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED, noRollbackFor = com.czspig.productcritic.common.BizException.class)
    public ApiResponse<ReviewDetailResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String owner = resolveSessionId(sessionId);
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        identity.limit("review:" + attrs.getRequest().getRemoteAddr(), 20, 3600);
        identity.limit("review:global", dailyLimit, 86400);
        if (owner.startsWith("anon:")) {
            db.queryForList("SELECT owner_key FROM anonymous_owners WHERE owner_key=? FOR UPDATE", owner);
            int used=db.queryForObject("SELECT successes FROM anonymous_owners WHERE owner_key=?", Integer.class, owner);
            if(used>=quota) throw new com.czspig.productcritic.common.BizException(com.czspig.productcritic.common.ErrorCode.RATE_LIMITED,"匿名体验额度已用完，项目已保留，请登录或注册继续");
        }
        if(owner.startsWith("account:")) db.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",Long.parseLong(owner.substring(8)));
        var result=reviewService.createReview(request, owner);
        if(owner.startsWith("anon:")) db.update("UPDATE anonymous_owners SET successes=successes+1 WHERE owner_key=?",owner);
        return ApiResponse.ok(result);
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.czspig.productcritic.auth.Identity identity;
    private String resolveSessionId(String ignored) {
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        return identity.owner(attrs.getRequest(), attrs.getResponse());
    }
}
