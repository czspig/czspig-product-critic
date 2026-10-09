package com.czspig.productcritic.auth;

import javax.sql.DataSource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Additive, idempotent migration; existing reviews and users are never rebuilt. */
@Component
public class AccountSchema implements InitializingBean {
    private final DataSource source;
    public AccountSchema(DataSource source) { this.source = source; }
    public void afterPropertiesSet() {
        new ResourceDatabasePopulator(new ClassPathResource("db/accounts-v1.sql"), new ClassPathResource("db/accounts-v2.sql")).execute(source);
    }
}
