package com.agentopscrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void productionAllowsVercelPreviewsAndConfiguredOrigin() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "corsAllowedOrigins", "https://agentops.example.com");

        List<String> origins = config.buildAllowedOrigins();

        assertTrue(origins.contains("https://agentops.example.com"));
        assertTrue(origins.contains("https://*.vercel.app"));
    }

    @Test
    void developmentIncludesLocalVite() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "dev");
        ReflectionTestUtils.setField(config, "corsAllowedOrigins", "");

        List<String> origins = config.buildAllowedOrigins();

        assertTrue(origins.contains("http://localhost:5173"));
        assertTrue(origins.contains("http://127.0.0.1:5173"));
    }
}
