package ru.dmitrii.jdbc;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@PropertySource(value = "classpath:/db.properties",encoding = "UTF-8")
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
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        liquibase.setDefaultSchema("public");
        liquibase.setDataSource(dataSource());
        return liquibase;
    }

    @PostConstruct
    public void makeDB() {
        try (Connection connection = DriverManager.getConnection(url, username, password);) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE  DATABASE chat");
            System.out.println("База данных chat создана");
        } catch (SQLException e) {
            System.out.println("База данных chat уже существует");
        }
    }
}