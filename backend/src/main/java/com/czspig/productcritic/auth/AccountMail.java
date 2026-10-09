package com.czspig.productcritic.auth;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import com.czspig.productcritic.common.*;

@Component
public class AccountMail {
    private final ObjectProvider<JavaMailSender> sender;
    private final boolean capture;
    private final String origin, from;
    private final boolean smtpConfigured;
    private final Map<String,Map<String,String>> inbox=new ConcurrentHashMap<>();
    public AccountMail(ObjectProvider<JavaMailSender> sender,Environment env,
        @Value("${app.mail.mode:smtp}") String mode,
        @Value("${app.mail.public-origin:http://127.0.0.1:5174}") String origin,
        @Value("${app.mail.from:}") String from) {
        this.sender=sender; this.origin=origin; this.from=from;
        this.smtpConfigured=!env.getProperty("spring.mail.host", "").isBlank();
        this.capture="capture".equals(mode) && Arrays.asList(env.getActiveProfiles()).contains("dev") && !Arrays.asList(env.getActiveProfiles()).contains("prod");
        if("capture".equals(mode)&&!capture) throw new IllegalStateException("Mail capture requires dev profile and cannot run with prod");
        URI uri=URI.create(origin);
        if(uri.getHost()==null || uri.getUserInfo()!=null || (!capture && !"https".equals(uri.getScheme()))) throw new IllegalStateException("Configure a trusted HTTPS mail public origin");
    }
    public void ready() { if(!capture && (!smtpConfigured || sender.getIfAvailable()==null || from.isBlank())) throw new BizException(ErrorCode.INTERNAL_ERROR,"邮件服务尚未配置，请联系管理员"); }
    public void send(String email,String purpose,String token) {
        ready(); String link=origin+"/account/"+(purpose.equals("VERIFY")?"verify":"reset")+"?token="+token;
        if(capture) {
            if(inbox.size()>500) inbox.clear();
            inbox.put(email,Map.of("email",email,"purpose",purpose,"link",link,"mode","开发邮件捕获（未发送真实邮件）")); return;
        }
        SimpleMailMessage mail=new SimpleMailMessage(); mail.setFrom(from); mail.setTo(email);
        mail.setSubject(purpose.equals("VERIFY")?"验证猪猪产品毒舌官邮箱":"重置猪猪产品毒舌官密码");
        mail.setText("请在 30 分钟内打开以下链接，链接只能使用一次：\n"+link);
        try { sender.getObject().send(mail); } catch(Exception e) { throw new BizException(ErrorCode.INTERNAL_ERROR,"邮件发送失败，请稍后重试"); }
    }
    public Map<String,String> captured(String email) { if(!capture) throw new BizException(ErrorCode.NOT_FOUND,"页面不存在"); return inbox.getOrDefault(email,Map.of()); }
}
