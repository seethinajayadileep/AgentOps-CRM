package com.agentopscrm.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Parses Railway {@code REDIS_URL} ({@code redis://} / {@code rediss://}) into
 * Lettuce connection settings.
 */
final class RedisUrl {

    record Parsed(String host, int port, String username, String password, boolean ssl) {}

    private RedisUrl() {
    }

    static Parsed parse(String redisUrl) {
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalArgumentException("REDIS_URL is blank");
        }
        String trimmed = redisUrl.trim();
        boolean ssl = trimmed.startsWith("rediss://");
        String httpShaped = trimmed
                .replaceFirst("^rediss://", "https://")
                .replaceFirst("^redis://", "http://");
        URI uri = URI.create(httpShaped);
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("REDIS_URL is missing a host");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 6379;
        String username = null;
        String password = null;
        if (uri.getRawUserInfo() != null && !uri.getRawUserInfo().isBlank()) {
            String[] parts = uri.getRawUserInfo().split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }
        if ("default".equals(username) && password != null) {
            username = null;
        }
        return new Parsed(uri.getHost(), port, username, password, ssl);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
