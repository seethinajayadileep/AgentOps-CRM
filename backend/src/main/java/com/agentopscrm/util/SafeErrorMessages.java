package com.agentopscrm.util;

import java.util.Locale;
import java.util.UUID;

/**
 * Maps internal exceptions to safe, user-facing messages. Technical details stay in logs
 * keyed by a short reference id.
 */
public final class SafeErrorMessages {

    public static final String PROVIDER_CONNECT =
            "Lead discovery could not connect to the provider. Check the integration and try again.";

    public static final String INTEGRATION_CONNECT =
            "The integration could not be reached. Check configuration and try again.";

    public static final String CREDENTIALS =
            "The provider rejected the stored credentials. Update the integration and try again.";

    public static final String GENERIC = "Something went wrong. Please try again.";

    public static final String CRAWL_FAILED =
            "Website crawl failed. Check the Firecrawl integration and try again.";

    public static final String KB_FAILED =
            "Knowledge base build failed. Check the embedding integration and try again.";

    public static final String DELETE_FAILED =
            "The business could not be deleted. No records were removed. Try again or contact support.";

    public static final String UNSUPPORTED_ANSWER =
            "The knowledge base does not contain confirmed information for that question.";

    private SafeErrorMessages() {
    }

    public static String newId() {
        return "ERR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public static String correlationIdFrom(UUID id) {
        if (id == null) {
            return newId();
        }
        return "ERR-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public static String forClient(Throwable error) {
        return forClient(error, newId());
    }

    public static String forClient(Throwable error, String errorId) {
        String safe = classify(error);
        return safe + " Reference " + errorId + ".";
    }

    public static boolean isTlsOrConnectivity(Throwable error) {
        String blob = flatten(error).toLowerCase(Locale.ROOT);
        return blob.contains("pkix")
                || blob.contains("certificate")
                || blob.contains("sslhandshake")
                || blob.contains("ssl exception")
                || blob.contains("unable to find valid certification path")
                || blob.contains("tls")
                || blob.contains("connection reset")
                || blob.contains("connect timed out")
                || blob.contains("unknownhost")
                || blob.contains("failed to connect");
    }

    public static String classify(Throwable error) {
        if (error == null) {
            return GENERIC;
        }
        if (isTlsOrConnectivity(error)) {
            return PROVIDER_CONNECT;
        }
        String message = error.getMessage() == null ? "" : error.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("unauthorized")
                || lower.contains("credentials were rejected")
                || lower.contains("401")
                || lower.contains("403")) {
            return CREDENTIALS;
        }
        return GENERIC;
    }

    public static String category(Throwable error) {
        if (error == null) {
            return "INTERNAL";
        }
        if (isTlsOrConnectivity(error)) {
            return "CONNECTIVITY";
        }
        String message = error.getMessage() == null ? "" : error.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("unauthorized") || lower.contains("401") || lower.contains("403")
                || lower.contains("credentials")) {
            return "CREDENTIALS";
        }
        if (lower.contains("not found") || lower.contains("404")) {
            return "NOT_FOUND";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "TIMEOUT";
        }
        return "INTERNAL";
    }

    public static String categoryFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "INTERNAL";
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("pkix") || lower.contains("certificate") || lower.contains("tls")
                || lower.contains("connect")) {
            return "CONNECTIVITY";
        }
        if (lower.contains("unauthorized") || lower.contains("credential") || lower.contains("401")) {
            return "CREDENTIALS";
        }
        if (lower.contains("timeout")) {
            return "TIMEOUT";
        }
        if (lower.contains("not found")) {
            return "NOT_FOUND";
        }
        return "INTERNAL";
    }

    public static String recommendedAction(String category) {
        if (category == null) {
            return "Retry the operation. If it continues to fail, review Settings and server logs using the reference id.";
        }
        return switch (category) {
            case "CONNECTIVITY" ->
                    "Verify outbound TLS and network access, then retry. If it persists, review server logs using the reference id.";
            case "CREDENTIALS" ->
                    "Update the integration credentials in Settings, then retry.";
            case "TIMEOUT" ->
                    "The provider took too long. Wait a moment and retry. A timeout in the browser does not mean the backend job failed.";
            case "NOT_FOUND" ->
                    "Confirm the record still exists, then retry.";
            default ->
                    "Retry the operation. If it continues to fail, review Settings and server logs using the reference id.";
        };
    }

    /**
     * User-facing sanitization: if the payload looks technical, replace it entirely.
     */
    public static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return GENERIC;
        }
        String redacted = redactSecrets(message);
        String lower = redacted.toLowerCase(Locale.ROOT);
        if (looksTechnical(lower)) {
            if (lower.contains("pkix") || lower.contains("certificate") || lower.contains("sslhandshake")
                    || lower.contains("tls") || lower.contains("javax.net")) {
                return PROVIDER_CONNECT;
            }
            return INTEGRATION_CONNECT;
        }
        return redacted;
    }

    /**
     * Redact secrets and URLs from structured JSON/text while keeping operator-useful fields.
     */
    public static String sanitizeJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String redacted = redactSecrets(json);
        redacted = redacted.replaceAll("(?i)https?://[^\\s\"'<>]+", "[redacted-url]");
        redacted = redacted.replaceAll("(?i)(javax\\.|java\\.|sun\\.|com\\.sun\\.|org\\.springframework\\.)[\\w.$]+", "[redacted]");
        redacted = redacted.replaceAll("(?i)PKIX[\\w\\s.:-]*", "[redacted]");
        return redacted;
    }

    public static String redactSecrets(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll(
                "(?i)(api[_-]?key|token|secret|password|authorization|bearer)[=:\\s]+\\S+",
                "$1=***");
    }

    private static boolean looksTechnical(String lower) {
        return lower.contains("pkix")
                || lower.contains("certificate")
                || lower.contains("javax.net")
                || lower.contains("java.")
                || lower.contains("sun.security")
                || lower.contains("http://")
                || lower.contains("https://")
                || lower.contains("stack")
                || lower.contains("caused by")
                || lower.contains("exception")
                || lower.contains("sslhandshake");
    }

    private static String flatten(Throwable error) {
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 8) {
            sb.append(current.getClass().getName()).append(' ');
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
            depth++;
        }
        return sb.toString();
    }
}
