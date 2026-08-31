package com.agentopscrm.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Railway's {@code DATABASE_URL} onto {@code DB_URL} / {@code DB_USER} /
 * {@code DB_PASSWORD} before {@code application-prod.yml} is bound.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RailwayDataSourceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String existing = firstNonBlank(
                environment.getProperty("DB_URL"),
                environment.getProperty("SPRING_DATASOURCE_URL"));
        boolean alreadyRemote = existing != null && !RailwayDatabaseUrl.looksLocal(existing);

        if (!alreadyRemote) {
            String raw = firstNonBlank(
                    environment.getProperty("DATABASE_PRIVATE_URL"),
                    environment.getProperty("DATABASE_URL"));
            if (raw != null) {
                RailwayDatabaseUrl.Parsed parsed = RailwayDatabaseUrl.parse(raw);
                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("DB_URL", parsed.jdbcUrl());
                properties.put("spring.datasource.url", parsed.jdbcUrl());
                if (parsed.username() != null
                        && firstNonBlank(environment.getProperty("DB_USER"), environment.getProperty("PGUSER")) == null) {
                    properties.put("DB_USER", parsed.username());
                    properties.put("spring.datasource.username", parsed.username());
                }
                if (parsed.password() != null
                        && firstNonBlank(environment.getProperty("DB_PASSWORD"), environment.getProperty("PGPASSWORD")) == null) {
                    properties.put("DB_PASSWORD", parsed.password());
                    properties.put("spring.datasource.password", parsed.password());
                }
                environment.getPropertySources().addFirst(new MapPropertySource("railwayDatasource", properties));
                existing = parsed.jdbcUrl();
            }
        }

        if (!isProd(environment)) {
            return;
        }
        String pgHost = environment.getProperty("PGHOST");
        boolean hasRemoteHost = pgHost != null && !pgHost.isBlank() && !isLocalHostName(pgHost);
        boolean hasRemoteUrl = existing != null && !RailwayDatabaseUrl.looksLocal(existing);
        if (!hasRemoteUrl && !hasRemoteHost) {
            throw new IllegalStateException(
                    "Production database is not configured. Link a Railway Postgres (pgvector) service "
                            + "or set DATABASE_URL / DB_URL. Without this the API never binds a port "
                            + "and Railway healthchecks fail as 'service unavailable'.");
        }
    }

    private static boolean isProd(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (profile != null && profile.toLowerCase().contains("prod")) {
                return true;
            }
        }
        String fromEnv = firstNonBlank(
                environment.getProperty("SPRING_PROFILES_ACTIVE"),
                environment.getProperty("spring.profiles.active"));
        return fromEnv != null && fromEnv.toLowerCase().contains("prod");
    }

    private static boolean isLocalHostName(String host) {
        String lower = host.toLowerCase();
        return "localhost".equals(lower) || "127.0.0.1".equals(lower);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
