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

    @org.springframework.beans.factory.annotation.Autowired private org.springframework.jdbc.core.JdbcTemplate db;
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
        String owner=resolveSessionId(sessionId);
        var project=reviewService.getProjectWorkbench(ideaGroupId,owner);
        var names=db.queryForList("SELECT name FROM project_names WHERE owner_key=? AND project_id=?",String.class,owner,ideaGroupId);
        if(!names.isEmpty()) project.setTitle(names.get(0));
        else {
            var summaries=db.queryForList("SELECT input_summary FROM review_records WHERE session_id=? AND idea_group_id=? AND deleted_at IS NULL ORDER BY version_no,id LIMIT 1",String.class,owner,ideaGroupId);
            if(!summaries.isEmpty()) project.setTitle(summaries.get(0).substring(0,Math.min(22,summaries.get(0).length())));
        }
        return ApiResponse.ok(project);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.czspig.productcritic.auth.Identity identity;
    private String resolveSessionId(String ignored) {
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        return identity.owner(attrs.getRequest(), attrs.getResponse());
    }
}
