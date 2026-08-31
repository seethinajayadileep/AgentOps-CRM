package com.agentopscrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS for the Vite SPA. Spring Security {@code cors(Customizer.withDefaults())}
 * picks up this {@link CorsConfigurationSource} so preflight and credentialed
 * requests from Vercel to Railway both receive the right headers.
 *
 * Credentials are required (session cookie + Authorization). Origins must be
 * explicit or patterns — never {@code *} with credentials.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        List<String> allowedOrigins = buildAllowedOrigins();
        config.setAllowedOriginPatterns(allowedOrigins);
        log.info("CORS allowed origin patterns: {}", allowedOrigins);

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Requested-With"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    List<String> buildAllowedOrigins() {
        List<String> origins = new ArrayList<>();

        if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
            for (String origin : corsAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }

        boolean production = activeProfile != null && activeProfile.toLowerCase().contains("prod");

        if (!production) {
            origins.add("http://localhost:5173");
            origins.add("http://127.0.0.1:5173");
            origins.add("http://localhost:3000");
            origins.add("http://127.0.0.1:3000");
        } else {
            origins.add("https://*.vercel.app");
        }

        if (origins.isEmpty()) {
            log.warn("No CORS origins configured — defaulting to local Vite. Set CORS_ALLOWED_ORIGINS in production.");
            origins.add("http://localhost:5173");
            origins.add("http://localhost:3000");
        }

        return origins;
    }
}
