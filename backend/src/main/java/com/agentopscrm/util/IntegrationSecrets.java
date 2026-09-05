package com.agentopscrm.util;

import java.net.URI;
import java.util.Locale;

/**
 * Shared checks for optional integration credentials. Never logs the values.
 */
public final class IntegrationSecrets {

    private IntegrationSecrets() {
    }

    public static boolean isUsableSecret(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return !trimmed.startsWith("your_")
                && !lower.contains("_here")
                && !trimmed.equals("...")
                && !trimmed.endsWith("...");
    }

    public static boolean isHttpsUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            if ("https".equals(scheme)) {
                return true;
            }
            return "http".equals(scheme) && ("localhost".equals(host) || "127.0.0.1".equals(host));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
