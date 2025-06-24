package org.mskcc.smile.persistence.jdbc;

import com.databricks.client.jdbc.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 *
 * @author laptop
 */
@Configuration
public class DatabricksJdbcConfig {
    @Value("${databricks.url}")
    private String databricksUrl;

    /**
     * Returns the databricks JdbcTemplate.
     * @return JdbcTemplate
     */
    @Bean
    public JdbcTemplate databricksJdbcTemplate() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(new Driver());
        dataSource.setUrl(databricksUrl);
        return new JdbcTemplate(dataSource);
    }
}
