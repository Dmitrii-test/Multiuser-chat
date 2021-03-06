package ru.dmitrii.jdbc;

import liquibase.integration.spring.SpringLiquibase;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ru.dmitrii.utils.UtilsConfiguration;

import javax.sql.DataSource;

@Configuration
@ComponentScan
@PropertySource(value = "classpath:/db.properties", encoding = "UTF-8")
@Import(UtilsConfiguration.class)
public class DataConfiguration {
    @Value("${db.driverClassName}")
    private String driverClassName;
    @Value("${db.url}")
    private String url;
    @Value("${db.urlDB}")
    private String urlDB;
    @Value("${db.username}")
    private String username;
    @Value("${db.password}")
    private String password;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    public DataSource dataSourceDB() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(urlDB);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public SimpleJdbcInsert simpleJdbcInsertUsers(JdbcTemplate jdbcTemplate) {
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingColumns("name", "password")
                .usingGeneratedKeyColumns("id_user");
    }

    @Bean
    public SimpleJdbcInsert simpleJdbcInsertMessages(JdbcTemplate jdbcTemplate) {
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("messages")
                .usingColumns("type", "data", "datetime", "user_id")
                .usingGeneratedKeyColumns("id_message");
    }

    @Bean
    public SpringLiquibase liquibase(@NotNull SpringLiquibase liquibaseDB) {
        liquibaseDB.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        liquibaseDB.setDataSource(dataSource());
        return liquibaseDB;
    }

    @Bean
    public SpringLiquibase liquibaseDB() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-base.xml");
        liquibase.setDefaultSchema("public");
        liquibase.setDataSource(dataSourceDB());
        return liquibase;
    }
}
