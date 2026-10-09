package com.czspig.productcritic.auth;

import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.czspig.productcritic.common.*;

@RestController
@RequestMapping("/api/project-directory")
public class ProjectDirectoryController {
    private final JdbcTemplate db; private final Identity identity;
    public ProjectDirectoryController(JdbcTemplate db,Identity identity) { this.db=db; this.identity=identity; }
    @GetMapping
    public ApiResponse<?> list(HttpServletRequest req,HttpServletResponse res,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="1") int page) {
        String owner=identity.owner(req,res); int offset=(Math.min(Math.max(page,1),10000)-1)*20;
        String pattern="%"+search.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String from=" FROM (SELECT idea_group_id, MIN(id) first_id, MAX(version_no) latest_version, MAX(updated_at) activity FROM review_records WHERE session_id=? AND deleted_at IS NULL GROUP BY idea_group_id) p JOIN review_records r ON r.id=p.first_id LEFT JOIN project_names n ON n.project_id=p.idea_group_id AND n.owner_key=? WHERE COALESCE(n.name,r.input_summary) LIKE ? ESCAPE '!'";
        List<Map<String,Object>> rows=db.queryForList("SELECT p.idea_group_id projectId, COALESCE(n.name,LEFT(r.input_summary,22)) name, p.latest_version latestVersion, p.activity updatedAt"+from+" ORDER BY p.activity DESC,p.first_id DESC LIMIT 20 OFFSET ?",owner,owner,pattern,offset);
        Long total=db.queryForObject("SELECT COUNT(*)"+from,Long.class,owner,owner,pattern);
        return ApiResponse.ok(Map.of("items",rows,"total",total,"page",Math.max(page,1)));
    }
    public record Rename(@NotBlank @Size(max=64) String name) {}
    private String own(String project,HttpServletRequest req,HttpServletResponse res) {
        String owner=identity.owner(req,res);
        if(owner.startsWith("account:")) db.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",Long.parseLong(owner.substring(8)));
        else db.queryForList("SELECT owner_key FROM anonymous_owners WHERE owner_key=? FOR UPDATE",owner);
        int count=db.queryForObject("SELECT COUNT(*) FROM review_records WHERE idea_group_id=? AND session_id=? AND deleted_at IS NULL",Integer.class,project,owner);
        if(count==0)throw new BizException(ErrorCode.NOT_FOUND,"项目不存在"); return owner;
    }
    @PatchMapping("/{project}") @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ApiResponse<?> rename(@PathVariable String project,@Valid @RequestBody Rename b,HttpServletRequest req,HttpServletResponse res) {
        String owner=own(project,req,res); db.update("INSERT INTO project_names(owner_key,project_id,name) VALUES (?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name)",owner,project,b.name().trim());
        db.update("UPDATE review_records SET updated_at=CURRENT_TIMESTAMP WHERE session_id=? AND idea_group_id=? AND deleted_at IS NULL",owner,project);
        return ApiResponse.ok(Map.of("message","项目名称已保存"));
    }
    @DeleteMapping("/{project}") @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ApiResponse<?> delete(@PathVariable String project,HttpServletRequest req,HttpServletResponse res) {
        String owner=own(project,req,res);
        db.update("UPDATE review_records SET deleted_at=CURRENT_TIMESTAMP WHERE idea_group_id=? AND session_id=? AND deleted_at IS NULL",project,owner);
        return ApiResponse.ok(Map.of("message","项目及其所有报告、版本已移至删除状态"));
    }
}
