package com.czspig.productcritic.controller;

import com.czspig.productcritic.common.ApiResponse;
import com.czspig.productcritic.dto.ProjectWorkbenchResponse;
import com.czspig.productcritic.service.ReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ReviewService reviewService;
    private final String anonymousSessionId;

    public ProjectController(
            ReviewService reviewService,
            @Value("${app.anonymous-session-id:anonymous-dev-session}") String anonymousSessionId) {
        this.reviewService = reviewService;
        this.anonymousSessionId = anonymousSessionId;
    }

    @GetMapping("/{ideaGroupId}")
    public ApiResponse<ProjectWorkbenchResponse> getProjectWorkbench(
            @PathVariable String ideaGroupId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ApiResponse.ok(reviewService.getProjectWorkbench(ideaGroupId, resolveSessionId(sessionId)));
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return anonymousSessionId;
        }
        return sessionId.trim();
    }
}
