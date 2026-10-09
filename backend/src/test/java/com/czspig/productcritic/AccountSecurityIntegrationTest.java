package com.czspig.productcritic;

import com.czspig.productcritic.auth.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Independent disposable accounts/projects only; never deletes existing data. Requires local MySQL. */
@SpringBootTest(properties={"app.ai.provider=mock","app.mail.mode=capture","app.mail.public-origin=http://127.0.0.1:5174"})
@ActiveProfiles("dev") @AutoConfigureMockMvc
class AccountSecurityIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired AccountMail mail; @Autowired JdbcTemplate db;
    @org.springframework.boot.test.mock.mockito.SpyBean com.czspig.productcritic.service.ReviewService service;
    static int clientNo=10;
    class Client {
        MockHttpSession session; Cookie anon; String csrf=""; String ip="2001:db8:"+Integer.toHexString(new Random().nextInt(65536))+":"+(++clientNo)+"::1";
        JsonNode call(String method,String path,Object body,int status) throws Exception { return call(method,path,body,status,null); }
        JsonNode call(String method,String path,Object body,int status,String header) throws Exception {
            var builder=request(org.springframework.http.HttpMethod.valueOf(method),path).with(r->{r.setRemoteAddr(ip);return r;});
            if(session!=null && !session.isInvalid())builder.session(session);
            if(anon!=null)builder.cookie(anon);
            if(!csrf.isEmpty())builder.header("X-CSRF-Token",csrf);
            if(header!=null)builder.header("X-Session-Id",header);
            if(body!=null)builder.contentType("application/json").content(json.writeValueAsString(body));
            var result=mvc.perform(builder).andReturn();
            assertEquals(status,result.getResponse().getStatus(),"Unexpected status on "+method+" "+path);
            session=(MockHttpSession)result.getRequest().getSession(false);
            for(String cookie:result.getResponse().getHeaders("Set-Cookie")) if(cookie.startsWith("PIG_ANON="))anon=new Cookie("PIG_ANON",cookie.split(";",2)[0].substring(9));
            JsonNode data=json.readTree(result.getResponse().getContentAsString()).path("data");
            if(data.has("csrf"))csrf=data.path("csrf").asText(); return data;
        }
        void boot() throws Exception { call("GET","/api/account/session",null,200); }
    }
    String password="TestOnly-Strong-2026";
    String createAccount(Client c) throws Exception {
        c.boot(); String name="test_"+UUID.randomUUID().toString().replace("-","").substring(0,16);
        c.call("POST","/api/account/register",Map.of("username",name,"nickname","独立测试","email",name+"@example.test","password",password),200);
        return name;
    }
    String token(String name) { String link=mail.captured(name+"@example.test").get("link"); return link.substring(link.indexOf("token=")+6); }
    void verify(Client c,String name) throws Exception { c.call("POST","/api/account/verify",Map.of("token",token(name)),200); }
    void login(Client c,String name) throws Exception { c.call("POST","/api/account/login",Map.of("username",name,"password",password),200); }
    JsonNode review(Client c,Map<String,Object> extra) throws Exception {
        Map<String,Object> body=new HashMap<>(Map.of("content","为独立测试制作一个用于校园失物招领的产品，先验证学生的实际需求。","mode","SHARP_PM","roastLevel",2)); body.putAll(extra); return c.call("POST","/api/reviews",body,200);
    }
    @Test void registrationTokensPasswordsAndSessions() throws Exception {
        Client a=new Client(); String name=createAccount(a); String t=token(name);
        a.call("POST","/api/account/login",Map.of("username",name,"password",password),400);
        a.call("POST","/api/account/register",Map.of("username",name.toUpperCase(),"nickname","duplicate","email","other@example.test","password",password),400);
        a.call("POST","/api/account/register",Map.of("username","other_"+name.substring(5),"nickname","duplicate","email",name+"@example.test","password",password),400);
        db.update("UPDATE account_tokens SET expires_at=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 MINUTE) WHERE token_hash=?",Identity.hash(t));
        a.call("POST","/api/account/verify",Map.of("token",t),400);
        a.call("POST","/api/account/resend",Map.of("email",name+"@example.test"),200); verify(a,name);
        a.call("POST","/api/account/verify",Map.of("token",token(name)),400);
        a.call("POST","/api/account/login",Map.of("username",name,"password","wrong"),400); login(a,name);
        assertEquals(name,a.call("GET","/api/account/session",null,200).path("user").path("username").asText());
        Client second=new Client(); second.boot(); login(second,name);
        a.call("POST","/api/account/password",Map.of("currentPassword","wrong","password","Changed-TestOnly-2026"),400);
        a.call("POST","/api/account/password",Map.of("currentPassword",password,"password","Changed-TestOnly-2026"),200);
        second.call("GET","/api/account/session",null,401);
        a.boot(); a.call("POST","/api/account/login",Map.of("username",name,"password",password),400);
        a.call("POST","/api/account/login",Map.of("username",name,"password","Changed-TestOnly-2026"),200);
        a.call("POST","/api/account/forgot",Map.of("email",name+"@example.test"),200); String reset=token(name);
        a.call("POST","/api/account/reset",Map.of("token",reset,"password",password),200);
        a.call("GET","/api/account/session",null,401); a.boot();
        a.call("POST","/api/account/reset",Map.of("token",reset,"password",password),400);
        login(a,name); a.call("POST","/api/account/logout",Map.of(),200); assertFalse(a.call("GET","/api/account/session",null,200).path("user").has("id"));
    }
    @Test void quotaMigrationAndOwnershipAreEnforced() throws Exception {
        Client a=new Client(); a.boot();
        org.mockito.Mockito.doThrow(new com.czspig.productcritic.common.BizException(com.czspig.productcritic.common.ErrorCode.AI_PROVIDER_ERROR,"测试模拟评审失败"))
            .when(service).createReview(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString());
        try {
            a.call("POST","/api/reviews",Map.of("content","只用于验证失败评审不会扣除成功额度。","mode","CLIENT","roastLevel",1),502);
            assertEquals(3,a.call("GET","/api/account/session",null,200).path("remaining").asInt());
        } finally { org.mockito.Mockito.reset(service); }
        JsonNode v1=review(a,Map.of()); String group=v1.path("ideaGroupId").asText(); long id=v1.path("id").asLong();
        review(a,Map.of("ideaGroupId",group,"parentReviewId",id)); JsonNode v3=review(a,Map.of("ideaGroupId",group,"parentReviewId",id));
        assertEquals(3,v3.path("versionNo").asInt()); assertEquals(id,v3.path("parentReviewId").asLong());
        a.call("POST","/api/reviews",Map.of("content","另一个独立校园产品测试项目内容。","mode","CLIENT","roastLevel",1),429);
        String name=createAccount(a); verify(a,name); login(a,name);
        assertEquals(0,a.call("GET","/api/project-directory",null,200).path("total").asInt()); // refusal leaves anonymous data separate
        assertEquals(1,a.call("POST","/api/account/migrate",Map.of(),200).path("projects").size());
        var repeated=a.call("POST","/api/account/migrate",Map.of(),200);
        assertEquals(0,repeated.path("migrated").asInt()); assertEquals(1,repeated.path("projects").size());
        assertEquals(3,a.call("GET","/api/reviews/group/"+group,null,200).path("versions").size());
        JsonNode v4=review(a,Map.of("ideaGroupId",group,"parentReviewId",id)); assertEquals(4,v4.path("versionNo").asInt()); assertEquals(id,v4.path("parentReviewId").asLong());
        List<Client> parallel=new ArrayList<>();
        for(int i=0;i<3;i++) { Client c=new Client(); c.boot(); login(c,name); parallel.add(c); }
        var pool=java.util.concurrent.Executors.newFixedThreadPool(3);
        try {
            List<java.util.concurrent.Future<JsonNode>> futures=new ArrayList<>();
            for(Client c:parallel) futures.add(pool.submit(()->review(c,Map.of("ideaGroupId",group,"parentReviewId",id))));
            Set<Integer> versions=new HashSet<>();
            for(var f:futures) { JsonNode next=f.get(); versions.add(next.path("versionNo").asInt()); assertEquals(id,next.path("parentReviewId").asLong()); }
            assertEquals(Set.of(5,6,7),versions);
        } finally { pool.shutdownNow(); }
        Client b=new Client(); String other=createAccount(b); verify(b,other); login(b,other);
        for(String path:List.of("/api/reviews/"+id,"/api/reviews/group/"+group,"/api/projects/"+group)) b.call("GET",path,null,404,"account:"+a.session.getAttribute("accountId"));
        b.call("PATCH","/api/project-directory/"+group,Map.of("name","attempt"),404);
        b.call("DELETE","/api/project-directory/"+group,null,404);
        b.call("POST","/api/reviews",Map.of("content","测试跨账号不能创建别人的后续版本。","mode","CLIENT","roastLevel",1,"parentReviewId",id,"ideaGroupId",group),404);
        Client anonymous=new Client(); anonymous.boot(); anonymous.call("GET","/api/reviews/"+id,null,404,"account:"+a.session.getAttribute("accountId"));
        a.call("PATCH","/api/project-directory/"+group,Map.of("name","隔离测试重命名"),200);
        assertEquals(1,a.call("GET","/api/project-directory?search=隔离测试",null,200).path("total").asInt());
        assertEquals(0,b.call("GET","/api/project-directory?search=隔离测试",null,200).path("total").asInt());
        String prefix="fixture-"+UUID.randomUUID(); String owner="account:"+a.session.getAttribute("accountId");
        for(int i=0;i<21;i++) db.update("INSERT INTO review_records(session_id,input_content,input_summary,idea_group_id,version_no,mode,roast_level,one_line_verdict,beat_score,positioning_score,report_json,report_markdown,status,model_name,prompt_version,created_at,updated_at) SELECT session_id,input_content,?, ?,1,mode,roast_level,one_line_verdict,beat_score,positioning_score,report_json,report_markdown,status,model_name,prompt_version,created_at,updated_at FROM review_records WHERE id=?",i==20?"全量搜索末页项目":"分页测试"+i,prefix+"-"+i,id);
        assertEquals(20,a.call("GET","/api/project-directory",null,200).path("items").size());
        assertEquals(2,a.call("GET","/api/project-directory?page=2",null,200).path("items").size());
        assertEquals(1,a.call("GET","/api/project-directory?search=全量搜索末页",null,200).path("items").size());
        db.update("UPDATE review_records SET deleted_at=CURRENT_TIMESTAMP WHERE session_id=? AND idea_group_id LIKE ?",owner,prefix+"%");
        a.call("DELETE","/api/project-directory/"+group,null,200); a.call("GET","/api/reviews/"+id,null,404);
        assertEquals(7,db.queryForObject("SELECT COUNT(*) FROM review_records WHERE session_id=? AND idea_group_id=? AND deleted_at IS NOT NULL",Integer.class,owner,group));
        a.call("POST","/api/account/logout",Map.of(),200); assertEquals(0,a.call("GET","/api/account/session",null,200).path("remaining").asInt());
    }
    @Test void csrfAndRateLimitRejectUntrustedRequests() throws Exception {
        Client c=new Client(); c.boot(); c.csrf="bad";
        c.call("POST","/api/account/login",Map.of("username","nobody","password","wrong"),403); c.boot();
        String unknown="nobody_"+UUID.randomUUID().toString().substring(0,8);
        for(int i=0;i<12;i++)c.call("POST","/api/account/login",Map.of("username",unknown,"password","wrong"),400);
        c.call("POST","/api/account/login",Map.of("username",unknown,"password","wrong"),429);
    }
}
