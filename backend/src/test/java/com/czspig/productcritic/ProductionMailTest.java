package com.czspig.productcritic;
import com.czspig.productcritic.auth.AccountMail;
import com.czspig.productcritic.common.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
class ProductionMailTest {
 @Test void missingSmtpFailsAndCaptureCannotRunInProduction() {
  var provider=new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);
  var env=new MockEnvironment(); env.setActiveProfiles("prod");
  var mail=new AccountMail(provider,env,"smtp","https://example.test","");
  assertThrows(BizException.class,mail::ready);
  assertThrows(IllegalStateException.class,()->new AccountMail(provider,env,"capture","https://example.test",""));
 }
}
