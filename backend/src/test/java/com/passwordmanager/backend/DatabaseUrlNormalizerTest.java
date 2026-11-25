package com.passwordmanager.backend;

import com.passwordmanager.backend.config.DatabaseUrlNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlNormalizerTest {

    @Test
    void normalizesNeonPostgresqlUrlPreservingSslAndChannelBinding() {
        String raw = "postgresql://demo_user:demo_pass@ep-example.c-6.us-east-2.aws.neon.tech/infravault?sslmode=require&channel_binding=require";
        DatabaseUrlNormalizer.NormalizedDataSource ds = DatabaseUrlNormalizer.normalize(raw, null, null);

        assertEquals(
                "jdbc:postgresql://ep-example.c-6.us-east-2.aws.neon.tech:5432/infravault?sslmode=require&channelBinding=require",
                ds.jdbcUrl()
        );
        assertEquals("demo_user", ds.username());
        assertEquals("demo_pass", ds.password());
    }

    @Test
    void normalizesJdbcUrlChannelBindingParam() {
        String raw = "jdbc:postgresql://ep-example.neon.tech/infravault?sslmode=require&channel_binding=require";
        DatabaseUrlNormalizer.NormalizedDataSource ds = DatabaseUrlNormalizer.normalize(raw, "u", "p");

        assertEquals(
                "jdbc:postgresql://ep-example.neon.tech/infravault?sslmode=require&channelBinding=require",
                ds.jdbcUrl()
        );
        assertEquals("u", ds.username());
        assertEquals("p", ds.password());
    }
}
