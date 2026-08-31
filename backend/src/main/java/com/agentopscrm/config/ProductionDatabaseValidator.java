package com.agentopscrm.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail fast in production if the datasource still points at localhost. That
 * usually means Railway Postgres was not linked, which otherwise looks like a
 * hung healthcheck.
 */
@Component
@Profile("prod")
public class ProductionDatabaseValidator implements InitializingBean {

    private final String datasourceUrl;

    public ProductionDatabaseValidator(@Value("${spring.datasource.url:}") String datasourceUrl) {
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    public void afterPropertiesSet() {
        validate(datasourceUrl);
    }

    static void validate(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank() || RailwayDatabaseUrl.looksLocal(jdbcUrl)) {
            throw new IllegalStateException(
                    "Production database is not configured. Link a Railway Postgres (pgvector) service "
                            + "or set DB_URL=jdbc:postgresql://HOST:PORT/DB (or DATABASE_URL).");
        }
    }
}
