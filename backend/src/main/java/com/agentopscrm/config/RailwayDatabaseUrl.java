package com.agentopscrm.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Converts Railway's {@code postgresql://} {@code DATABASE_URL} into a Spring
 * JDBC URL. Railway does not inject {@code DB_URL} unless you set it yourself.
 */
final class RailwayDatabaseUrl {

    record Parsed(String jdbcUrl, String username, String password) {}

    private RailwayDatabaseUrl() {
    }

    static Parsed parse(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is blank");
        }
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql:")) {
            return new Parsed(trimmed, null, null);
        }
        String httpShaped = trimmed
                .replaceFirst("^postgresql://", "http://")
                .replaceFirst("^postgres://", "http://");
        URI uri = URI.create(httpShaped);
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("DATABASE_URL is missing a host");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/railway" : uri.getPath();
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + path;
        String query = uri.getRawQuery();
        if (query != null && !query.isBlank()) {
            jdbc = jdbc + "?" + query;
        } else if (needsSsl(uri.getHost())) {
            jdbc = jdbc + "?sslmode=require";
        }
        String username = null;
        String password = null;
        if (uri.getRawUserInfo() != null && !uri.getRawUserInfo().isBlank()) {
            String[] parts = uri.getRawUserInfo().split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }
        return new Parsed(jdbc, username, password);
    }

    static boolean looksLocal(String jdbcUrl) {
        if (jdbcUrl == null) {
            return true;
        }
        String lower = jdbcUrl.toLowerCase();
        return lower.contains("://localhost") || lower.contains("://127.0.0.1");
    }

    private static boolean needsSsl(String host) {
        String lower = host.toLowerCase();
        return lower.contains("rlwy.net") || lower.contains("proxy.rlwy") || lower.endsWith("railway.app");
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
