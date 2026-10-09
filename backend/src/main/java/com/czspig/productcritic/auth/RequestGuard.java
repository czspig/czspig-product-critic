package com.czspig.productcritic.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.czspig.productcritic.common.*;

@Component
public class RequestGuard extends OncePerRequestFilter {
    private final Identity identity;
    public RequestGuard(Identity identity) { this.identity=identity; }
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(!req.getRequestURI().startsWith("/api/")) { chain.doFilter(req,res); return; }
        res.setHeader("Cache-Control","no-store");
        try {
            identity.limit("api:"+req.getRemoteAddr(),300,60);
            if(!java.util.Set.of("GET","HEAD","OPTIONS").contains(req.getMethod())) {
                HttpSession s=req.getSession(false); String provided=req.getHeader("X-CSRF-Token");
                if(s==null || provided==null || s.getAttribute("csrf")==null || !MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),s.getAttribute("csrf").toString().getBytes(StandardCharsets.UTF_8))) throw new BizException(ErrorCode.FORBIDDEN,"页面验证已失效，请刷新后重试");
            }
            chain.doFilter(req,res);
        } catch(BizException e) {
            res.setStatus(e.getErrorCode().getHttpStatus().value()); res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"success\":false,\"code\":\""+e.getErrorCode().getCode()+"\",\"message\":\""+e.getMessage()+"\"}");
        }
    }
}
