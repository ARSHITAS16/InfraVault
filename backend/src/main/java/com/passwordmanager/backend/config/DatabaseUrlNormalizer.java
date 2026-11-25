package com.passwordmanager.backend.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Normalizes PostgreSQL connection URLs (libpq postgres://, postgresql://, or jdbc:postgresql://)
 * into JDBC-compatible URLs while preserving SSL and channel-binding parameters for Neon.
 */
public final class DatabaseUrlNormalizer {

    private DatabaseUrlNormalizer() {
    }

    public record NormalizedDataSource(String jdbcUrl, String username, String password) {
    }

    public static void applyEnvironmentNormalization() {
        String rawUrl = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("DB_URL")
        );
        String explicitUser = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                System.getenv("DB_USERNAME")
        );
        String explicitPass = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_PASSWORD"),
                System.getenv("DB_PASSWORD")
        );

        if (rawUrl != null) {
            NormalizedDataSource normalized = normalize(rawUrl, explicitUser, explicitPass);
            System.setProperty("spring.datasource.url", normalized.jdbcUrl());
            if (normalized.username() != null && !normalized.username().isBlank()) {
                System.setProperty("spring.datasource.username", normalized.username());
            }
            if (normalized.password() != null && !normalized.password().isBlank()) {
                System.setProperty("spring.datasource.password", normalized.password());
            }
        } else {
            if (explicitUser != null) {
                System.setProperty("spring.datasource.username", explicitUser);
            }
            if (explicitPass != null) {
                System.setProperty("spring.datasource.password", explicitPass);
            }
        }
    }

    public static NormalizedDataSource normalize(String rawUrl, String explicitUser, String explicitPass) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Database URL must not be blank");
        }

        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            String fixed = normalizeJdbcQuery(trimmed);
            return new NormalizedDataSource(fixed, explicitUser, explicitPass);
        }

        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            String httpLike = trimmed.startsWith("postgres://")
                    ? "http://" + trimmed.substring("postgres://".length())
                    : "http://" + trimmed.substring("postgresql://".length());

            URI uri = URI.create(httpLike);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Database URL is missing host");
            }
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/postgres";
            }

            String username = isDefaultDevUser(explicitUser) ? null : explicitUser;
            String password = isDefaultDevPassword(explicitPass) ? null : explicitPass;
            String rawUserInfo = uri.getRawUserInfo();
            if (rawUserInfo != null && !rawUserInfo.isBlank()) {
                String[] parts = rawUserInfo.split(":", 2);
                if (isBlank(username) && parts.length >= 1) {
                    username = decode(parts[0]);
                }
                if (isBlank(password) && parts.length == 2) {
                    password = decode(parts[1]);
                }
            }

            String query = normalizeQueryParams(uri.getRawQuery(), host);
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query.isEmpty() ? "" : "?" + query);
            return new NormalizedDataSource(jdbcUrl, username, password);
        }

        return new NormalizedDataSource(trimmed, explicitUser, explicitPass);
    }

    private static boolean isDefaultDevUser(String u) {
        return isBlank(u) || "password_admin".equals(u);
    }

    private static boolean isDefaultDevPassword(String p) {
        return isBlank(p) || "change_me_dev_only".equals(p);
    }

    private static String normalizeJdbcQuery(String jdbcUrl) {
        int qIdx = jdbcUrl.indexOf('?');
        if (qIdx == -1) {
            return jdbcUrl;
        }
        String base = jdbcUrl.substring(0, qIdx);
        String rawQuery = jdbcUrl.substring(qIdx + 1);
        String hostPart = base.substring("jdbc:postgresql://".length());
        String query = normalizeQueryParams(rawQuery, hostPart);
        return query.isEmpty() ? base : base + "?" + query;
    }

    private static String normalizeQueryParams(String rawQuery, String hostOrAuthority) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                if (pair.isBlank()) continue;
                String[] kv = pair.split("=", 2);
                String key = decode(kv[0]);
                String val = kv.length == 2 ? decode(kv[1]) : "";
                if ("channel_binding".equalsIgnoreCase(key)) {
                    key = "channelBinding";
                }
                params.put(key, val);
            }
        }
        if (hostOrAuthority != null && hostOrAuthority.contains("neon.tech") && !params.containsKey("sslmode")) {
            params.put("sslmode", "require");
        }
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a)) return a.trim();
        if (!isBlank(b)) return b.trim();
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
