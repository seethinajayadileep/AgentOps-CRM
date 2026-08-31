package com.agentopscrm.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailwayDatabaseUrlTest {

    @Test
    void convertsRailwayPrivateUrl() {
        RailwayDatabaseUrl.Parsed parsed = RailwayDatabaseUrl.parse(
                "postgresql://postgres:s3cret@postgres.railway.internal:5432/railway");
        assertEquals("jdbc:postgresql://postgres.railway.internal:5432/railway", parsed.jdbcUrl());
        assertEquals("postgres", parsed.username());
        assertEquals("s3cret", parsed.password());
        assertFalse(RailwayDatabaseUrl.looksLocal(parsed.jdbcUrl()));
    }

    @Test
    void addsSslForPublicProxyHost() {
        RailwayDatabaseUrl.Parsed parsed = RailwayDatabaseUrl.parse(
                "postgresql://postgres:p%40ss@turntable.proxy.rlwy.net:12345/railway");
        assertEquals("jdbc:postgresql://turntable.proxy.rlwy.net:12345/railway?sslmode=require", parsed.jdbcUrl());
        assertEquals("p@ss", parsed.password());
    }

    @Test
    void keepsExplicitJdbcUrl() {
        RailwayDatabaseUrl.Parsed parsed = RailwayDatabaseUrl.parse(
                "jdbc:postgresql://db.example.com:5432/agentops_crm");
        assertEquals("jdbc:postgresql://db.example.com:5432/agentops_crm", parsed.jdbcUrl());
    }

    @Test
    void flagsLocalhost() {
        assertTrue(RailwayDatabaseUrl.looksLocal("jdbc:postgresql://localhost:5432/railway"));
        assertTrue(RailwayDatabaseUrl.looksLocal("jdbc:postgresql://127.0.0.1:5433/agentops_crm"));
    }
}

class ProductionDatabaseValidatorTest {

    @Test
    void rejectsLocalhostInProduction() {
        assertThrows(IllegalStateException.class, () -> ProductionDatabaseValidator.validate(
                "jdbc:postgresql://localhost:5432/railway"));
        assertThrows(IllegalStateException.class, () -> ProductionDatabaseValidator.validate(""));
    }

    @Test
    void acceptsRailwayHost() {
        ProductionDatabaseValidator.validate("jdbc:postgresql://postgres.railway.internal:5432/railway");
    }
}
