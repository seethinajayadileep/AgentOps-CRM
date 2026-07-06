package com.agentopscrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration
 * 
 * Environment-controlled CORS origins for development and production.
 * 
 * Development: Includes localhost origins automatically
 * Production: Uses CORS_ALLOWED_ORIGINS environment variable
 * 
 * Security: Does NOT use wildcard (*) with credentials
 * 
 * @author AgentOps Team
 * @version 1.0.0
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.java);

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Build allowed origins list
        List<String> allowedOrigins = buildAllowedOrigins();
        config.setAllowedOriginPatterns(allowedOrigins);

        log.info("CORS Configuration initialized with {} allowed origins", allowedOrigins.size());
        allowedOrigins.forEach(origin -> log.debug("CORS allowed origin: {}", origin));

        // Allowed HTTP methods
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Allowed headers
        config.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Requested-With"
        ));

        // Expose headers
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));

        // Pre-flight cache duration (1 hour)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * Build allowed origins list based on environment
     * 
     * Development: Includes localhost automatically
     * Production: Uses CORS_ALLOWED_ORIGINS environment variable
     */
    private List<String> buildAllowedOrigins() {
        List<String> origins = new ArrayList<>();

        // Add environment-configured origins (comma-separated)
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
            String[] configuredOrigins = corsAllowedOrigins.split(",");
            for (String origin : configuredOrigins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }  
            }
        }

        // Add localhost origins in development profile
        if ("dev".equalsIgnoreCase(activeProfile)) {
            origins.add("http://localhost:5173");
            origins.add("http://127.0.0.1:5173");
            origins.add("http://localhost:3000");
            origins.add("http://127.0.0.1:3000");
        }

        // Fallback if no origins configured (development only!)
        if (origins.isEmpty()) {
            log.warn("No CORS origins configured - defaulting to localhost (NOT safe for production!)");
            origins.add("http://localhost:5173");
            origins.add("http://localhost:3000");
        }

        return origins;
    }
}