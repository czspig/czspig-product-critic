package com.czspig.productcritic.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.czspig.productcritic.common.*;

@Component
public class Identity {
    private final JdbcTemplate db;
    private final boolean secure;
    private final SecureRandom random = new SecureRandom();
    public Identity(JdbcTemplate db, @Value("${app.auth.secure-cookie:false}") boolean secure, org.springframework.core.env.Environment env) {
        this.db=db; this.secure=secure;
        if(Arrays.asList(env.getActiveProfiles()).contains("prod") && !secure) throw new IllegalStateException("Production requires secure cookies and HTTPS");
    }
    public String randomToken() { byte[] b=new byte[32]; random.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    public static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    public String anonymous(HttpServletRequest req, HttpServletResponse res) {
        if(req.getAttribute("anonymousOwner") instanceof String s) return s;
        String cookie=null;
        if(req.getCookies()!=null) for(Cookie c:req.getCookies()) if(c.getName().equals("PIG_ANON")) cookie=c.getValue();
        List<String> owners = cookie==null ? List.of() : db.queryForList("SELECT owner_key FROM anonymous_owners WHERE token_hash=?",String.class,hash(cookie));
        String owner;
        if(owners.isEmpty()) {
            String token=randomToken(); owner="anon:"+randomToken();
            db.update("INSERT INTO anonymous_owners(token_hash,owner_key) VALUES (?,?)",hash(token),owner);
            res.addHeader("Set-Cookie","PIG_ANON="+token+"; Path=/; HttpOnly; SameSite=Lax; Max-Age=31536000"+(secure?"; Secure":""));
        } else owner=owners.get(0);
        req.setAttribute("anonymousOwner",owner); return owner;
    }
    public Map<String,Object> user(HttpServletRequest req) {
        HttpSession s=req.getSession(false);
        if(s==null || s.getAttribute("accountId")==null) return null;
        List<Map<String,Object>> rows=db.queryForList("SELECT id,username,nickname,email,verified,session_version FROM accounts WHERE id=? AND verified=TRUE",s.getAttribute("accountId"));
        if(rows.isEmpty() || !rows.get(0).get("session_version").equals(s.getAttribute("accountVersion"))) { s.invalidate(); throw new BizException(ErrorCode.UNAUTHORIZED,"登录已失效，请重新登录"); }
        return rows.get(0);
    }
    public Map<String,Object> requireUser(HttpServletRequest req) { Map<String,Object> u=user(req); if(u==null) throw new BizException(ErrorCode.UNAUTHORIZED,"请先登录"); return u; }
    public String owner(HttpServletRequest req,HttpServletResponse res) { Map<String,Object> u=user(req); return u==null?anonymous(req,res):"account:"+u.get("id"); }
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void limit(String key,int max,long seconds) {
        String k=hash(key); long window=System.currentTimeMillis()/(seconds*1000);
        db.update("INSERT INTO account_rate_limits(bucket_key,window_start,attempts) VALUES (?,?,1) ON DUPLICATE KEY UPDATE attempts=IF(window_start=VALUES(window_start),attempts+1,1), window_start=VALUES(window_start)",k,window);
        Integer count=db.queryForObject("SELECT attempts FROM account_rate_limits WHERE bucket_key=?",Integer.class,k);
        if(count>max) throw new BizException(ErrorCode.RATE_LIMITED,"操作过于频繁，请稍后重试");
    }
}
