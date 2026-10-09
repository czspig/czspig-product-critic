package com.czspig.productcritic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.mail.mode=smtp","app.mail.public-origin=https://example.test","app.mail.from=","spring.mail.host=","app.auth.secure-cookie=true"})
@ActiveProfiles("prod") @AutoConfigureMockMvc
class ProductionMailIntegrationTest {
 @Autowired MockMvc mvc;
 @Test void registerReportsMissingProductionMailInsteadOfFakeSuccess() throws Exception {
  MockHttpSession s=new MockHttpSession(); s.setAttribute("csrf","production-test-only");
  mvc.perform(post("/api/account/register").session(s).header("X-CSRF-Token","production-test-only").with(r->{r.setRemoteAddr("127.0.0.99");return r;}).contentType("application/json")
    .content("{\"username\":\"mail_config_test\",\"nickname\":\"测试\",\"email\":\"mail_config@example.test\",\"password\":\"TestOnly-Strong-2026\"}"))
    .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message").value("邮件服务尚未配置，请联系管理员"));
 }
}
