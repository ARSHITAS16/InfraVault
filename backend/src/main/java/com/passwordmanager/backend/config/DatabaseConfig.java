package com.passwordmanager.backend.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url != null) {
            if (url.startsWith("postgres://")) {
                url = "jdbc:postgresql://" + url.substring(11);
            } else if (url.startsWith("postgresql://")) {
                url = "jdbc:postgresql://" + url.substring(13);
            }
            properties.setUrl(url);
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
