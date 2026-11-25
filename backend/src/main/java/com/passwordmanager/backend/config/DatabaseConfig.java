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
        if (url != null && !url.isBlank()) {
            DatabaseUrlNormalizer.NormalizedDataSource normalized = DatabaseUrlNormalizer.normalize(
                    url,
                    properties.getUsername(),
                    properties.getPassword()
            );
            properties.setUrl(normalized.jdbcUrl());
            if (normalized.username() != null && !normalized.username().isBlank()) {
                properties.setUsername(normalized.username());
            }
            if (normalized.password() != null && !normalized.password().isBlank()) {
                properties.setPassword(normalized.password());
            }
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
