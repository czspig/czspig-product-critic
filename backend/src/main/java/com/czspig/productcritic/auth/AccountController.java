package com.czspig.productcritic.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import com.czspig.productcritic.common.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final JdbcTemplate db; private final Identity identity; private final AccountMail mail;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
    private final String dummyHash=passwords.encode("timing-only-dummy-password");
    private final int quota;
    public AccountController(JdbcTemplate db,Identity identity,AccountMail mail,@org.springframework.beans.factory.annotation.Value("${app.auth.anonymous-quota:3}") int quota) { this.db=db; this.identity=identity; this.mail=mail; this.quota=quota; }
    public record Register(@NotBlank @Pattern(regexp="[A-Za-z0-9_]{3,32}",message="用户名需为3到32位字母、数字或下划线") String username,@NotBlank @Size(max=64) String nickname,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=10,max=72) String password) {}
    public record Login(@NotBlank @Size(max=32) String username,@NotBlank @Size(max=72) String password) {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record TokenRequest(@NotBlank @Size(max=128) String token,@Size(min=10,max=72) String password) {}
    public record PasswordRequest(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(min=10,max=72) String password) {}
    public record NicknameRequest(@NotBlank @Size(max=64) String nickname) {}
    private String normalize(String text) { return text.trim().toLowerCase(Locale.ROOT); }
    private void rate(HttpServletRequest req,String action,String key,int n) { identity.limit(action+":ip:"+req.getRemoteAddr(),n,900); identity.limit(action+":subject:"+normalize(key),n,900); }
    private Map<String,Object> publicUser(Map<String,Object> u) { if(u==null)return Map.of(); return Map.of("id",u.get("id"),"username",u.get("username"),"nickname",u.get("nickname"),"email",u.get("email"),"verified",true); }
    @GetMapping("/session")
    public ApiResponse<?> session(HttpServletRequest req,HttpServletResponse res) {
        String anon=identity.anonymous(req,res); Map<String,Object> u=identity.user(req); HttpSession session=req.getSession();
        if(session.getAttribute("csrf")==null) session.setAttribute("csrf",identity.randomToken());
        int used=db.queryForObject("SELECT successes FROM anonymous_owners WHERE owner_key=?",Integer.class,anon);
        int projects=db.queryForObject("SELECT COUNT(DISTINCT idea_group_id) FROM review_records WHERE session_id=? AND deleted_at IS NULL",Integer.class,anon);
        return ApiResponse.ok(Map.of("user",publicUser(u),"csrf",session.getAttribute("csrf"),"identityKey",u==null?anon:"account:"+u.get("id"),"anonymousKey",anon,"remaining",Math.max(0,quota-used),"anonymousProjects",projects));
    }
    @PostMapping("/register") @Transactional
    public ApiResponse<?> register(@Valid @RequestBody Register body,HttpServletRequest req) {
        checkPassword(body.password());
        rate(req,"register",body.email(),5); mail.ready();
        try { db.update("INSERT INTO accounts(username,nickname,email,password_hash) VALUES (?,?,?,?)",normalize(body.username()),body.nickname().trim(),normalize(body.email()),passwords.encode(body.password())); }
        catch(DuplicateKeyException e) { throw new BizException(ErrorCode.VALIDATION_ERROR,"无法完成注册，请检查用户名或邮箱，或使用登录／找回密码"); }
        Long id=db.queryForObject("SELECT id FROM accounts WHERE username=?",Long.class,normalize(body.username()));
        issue(id,normalize(body.email()),"VERIFY"); return ApiResponse.ok(Map.of("message","请验证邮箱后登录"));
    }
    private void issue(Long id,String email,String purpose) {
        String token=identity.randomToken(); db.update("UPDATE account_tokens SET used=TRUE WHERE account_id=? AND purpose=?",id,purpose);
        db.update("INSERT INTO account_tokens(token_hash,account_id,purpose,expires_at) VALUES (?,?,?,?)",Identity.hash(token),id,purpose,Timestamp.from(Instant.now().plusSeconds(1800)));
        mail.send(email,purpose,token);
    }
    @PostMapping("/resend") @Transactional
    public ApiResponse<?> resend(@Valid @RequestBody EmailRequest b,HttpServletRequest req) { return sendExisting(b,req,"VERIFY"); }
    @PostMapping("/forgot") @Transactional
    public ApiResponse<?> forgot(@Valid @RequestBody EmailRequest b,HttpServletRequest req) { return sendExisting(b,req,"RESET"); }
    private ApiResponse<?> sendExisting(EmailRequest b,HttpServletRequest req,String purpose) {
        rate(req,"mail",b.email(),5); mail.ready();
        List<Map<String,Object>> rows=db.queryForList("SELECT id,email FROM accounts WHERE email=? AND verified=?",normalize(b.email()),purpose.equals("RESET"));
        if(!rows.isEmpty()) issue(((Number)rows.get(0).get("id")).longValue(),rows.get(0).get("email").toString(),purpose);
        return ApiResponse.ok(Map.of("message","如果邮箱符合条件，将收到邮件。请检查收件箱。"));
    }
    private Long consume(String token,String purpose) {
        List<Map<String,Object>> rows=db.queryForList("SELECT account_id FROM account_tokens WHERE token_hash=? AND purpose=? AND used=FALSE AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",Identity.hash(token),purpose);
        if(rows.isEmpty()) throw new BizException(ErrorCode.VALIDATION_ERROR,"链接无效、已使用或已过期，请重新申请");
        db.update("UPDATE account_tokens SET used=TRUE WHERE token_hash=?",Identity.hash(token)); return ((Number)rows.get(0).get("account_id")).longValue();
    }
    @PostMapping("/verify") @Transactional
    public ApiResponse<?> verify(@Valid @RequestBody TokenRequest b,HttpServletRequest req) {
        rate(req,"verify",b.token(),10); Long id=consume(b.token(),"VERIFY"); db.update("UPDATE accounts SET verified=TRUE WHERE id=?",id); return ApiResponse.ok(Map.of("message","邮箱验证成功，请登录"));
    }
    @PostMapping("/reset") @Transactional
    public ApiResponse<?> reset(@Valid @RequestBody TokenRequest b,HttpServletRequest req) {
        rate(req,"reset",b.token(),10); if(b.password()==null)throw new BizException(ErrorCode.VALIDATION_ERROR,"请输入新密码");
        checkPassword(b.password()); Long id=consume(b.token(),"RESET"); db.update("UPDATE accounts SET password_hash=?,session_version=session_version+1 WHERE id=?",passwords.encode(b.password()),id);
        db.update("UPDATE account_tokens SET used=TRUE WHERE account_id=? AND purpose='RESET'",id); return ApiResponse.ok(Map.of("message","密码已重置，请重新登录"));
    }
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody Login b,HttpServletRequest req,HttpServletResponse res) {
        checkPassword(b.password());
        rate(req,"login",b.username(),12); identity.anonymous(req,res);
        List<Map<String,Object>> rows=db.queryForList("SELECT * FROM accounts WHERE username=? AND verified=TRUE",normalize(b.username()));
        String hash=rows.isEmpty()?dummyHash:rows.get(0).get("password_hash").toString(); boolean valid=passwords.matches(b.password(),hash);
        if(rows.isEmpty()||!valid)throw new BizException(ErrorCode.VALIDATION_ERROR,"用户名或密码错误，或邮箱尚未验证");
        HttpSession old=req.getSession(false); if(old!=null) old.invalidate(); HttpSession s=req.getSession(true);
        s.setAttribute("accountId",rows.get(0).get("id")); s.setAttribute("accountVersion",rows.get(0).get("session_version")); s.setAttribute("csrf",identity.randomToken());
        return session(req,res);
    }
    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest req) { HttpSession s=req.getSession(false); if(s!=null)s.invalidate(); return ApiResponse.ok(Map.of("message","已退出登录")); }
    @PatchMapping("/profile")
    public ApiResponse<?> profile(@Valid @RequestBody NicknameRequest b,HttpServletRequest req) { Map<String,Object> u=identity.requireUser(req); db.update("UPDATE accounts SET nickname=? WHERE id=?",b.nickname().trim(),u.get("id")); return ApiResponse.ok(Map.of("message","昵称已保存")); }
    @PostMapping("/password") @Transactional
    public ApiResponse<?> password(@Valid @RequestBody PasswordRequest b,HttpServletRequest req) {
        checkPassword(b.password());
        Map<String,Object> u=identity.requireUser(req); rate(req,"password",u.get("id").toString(),10);
        String hash=db.queryForObject("SELECT password_hash FROM accounts WHERE id=? FOR UPDATE",String.class,u.get("id"));
        if(!passwords.matches(b.currentPassword(),hash)) throw new BizException(ErrorCode.VALIDATION_ERROR,"当前密码不正确");
        db.update("UPDATE accounts SET password_hash=?,session_version=session_version+1 WHERE id=?",passwords.encode(b.password()),u.get("id")); req.getSession().invalidate();
        return ApiResponse.ok(Map.of("message","密码已修改，请重新登录"));
    }
    @PostMapping("/migrate") @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ApiResponse<?> migrate(HttpServletRequest req,HttpServletResponse res) {
        Map<String,Object> u=identity.requireUser(req); String anon=identity.anonymous(req,res), owner="account:"+u.get("id");
        db.queryForList("SELECT owner_key FROM anonymous_owners WHERE owner_key=? FOR UPDATE",anon);
        db.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",u.get("id"));
        List<String> groups=db.queryForList("SELECT DISTINCT idea_group_id FROM review_records WHERE session_id=?",String.class,anon);
        for(String group:groups) db.update("INSERT IGNORE INTO anonymous_project_migrations(anonymous_owner,project_id,account_id) VALUES (?,?,?)",anon,group,u.get("id"));
        db.update("UPDATE review_records SET session_id=?,user_id=? WHERE session_id=?",owner,u.get("id"),anon);
        db.update("UPDATE project_names SET owner_key=? WHERE owner_key=?",owner,anon);
        List<String> receipt=db.queryForList("SELECT project_id FROM anonymous_project_migrations WHERE anonymous_owner=? AND account_id=?",String.class,anon,u.get("id"));
        return ApiResponse.ok(Map.of("projects",receipt,"migrated",groups.size()));
    }
    @GetMapping("/dev-mail")
    public ApiResponse<?> devMail(@RequestParam String email,HttpServletRequest req) {
        if(!Set.of("127.0.0.1","0:0:0:0:0:0:0:1","::1").contains(req.getRemoteAddr()))throw new BizException(ErrorCode.NOT_FOUND,"页面不存在");
        return ApiResponse.ok(mail.captured(normalize(email)));
    }
    private void checkPassword(String password) {
        if(password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new BizException(ErrorCode.VALIDATION_ERROR,"密码过长，请使用不超过72字节的密码");
    }
}
