package com.agentopscrm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeErrorMessagesTest {

    @Test
    void tlsFailuresMapToProviderConnectMessage() {
        Exception tls = new Exception("PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException");
        String client = SafeErrorMessages.forClient(tls, "ERR-TEST01");
        assertTrue(client.startsWith(SafeErrorMessages.PROVIDER_CONNECT));
        assertTrue(client.contains("ERR-TEST01"));
        assertFalse(client.toLowerCase().contains("pkix"));
        assertFalse(client.contains("sun.security"));
    }

    @Test
    void sanitizeStripsTokensAndUrls() {
        String raw = "Failed https://api.apify.com/v2 token=apify_api_secret123 Caused by: javax.net.ssl.SSLHandshakeException";
        String safe = SafeErrorMessages.sanitize(raw);
        assertEquals(SafeErrorMessages.PROVIDER_CONNECT, safe);
        assertFalse(safe.contains("apify_api"));
        assertFalse(safe.contains("https://"));
    }

    @Test
    void sanitizeJsonRedactsUrlsAndSecretsWithoutDroppingStatus() {
        String json = "{\"status\":\"FAILED\",\"error\":\"https://api.openai.com/v1 token=sk-secret\"}";
        String safe = SafeErrorMessages.sanitizeJson(json);
        assertTrue(safe.contains("FAILED"));
        assertFalse(safe.contains("https://"));
        assertFalse(safe.contains("sk-secret"));
    }

    @Test
    void genericErrorsDoNotLeakClassNames() {
        Exception boom = new IllegalStateException("com.agentopscrm.client.ApifyClient$ApifyException: boom");
        String client = SafeErrorMessages.classify(boom);
        assertEquals(SafeErrorMessages.GENERIC, client);
    }
}
