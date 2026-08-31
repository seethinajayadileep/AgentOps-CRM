package com.agentopscrm.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail fast in production if the JWT signing secret is missing or still the
 * local development placeholder. Railway must set {@code JWT_SECRET}.
 */
@Component
@Profile("prod")
public class ProductionSecurityValidator implements InitializingBean {

    static final String DEV_PLACEHOLDER = "change-this-demo-secret-to-a-long-random-value";
    static final int MIN_LENGTH = 32;

    private final String jwtSecret;

    public ProductionSecurityValidator(@Value("${jwt.secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public void afterPropertiesSet() {
        validateJwtSecret(jwtSecret);
    }

    static void validateJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set in production. Generate one with: openssl rand -base64 64");
        }
        if (secret.equals(DEV_PLACEHOLDER) || secret.contains("change-this-demo-secret")) {
            throw new IllegalStateException(
                    "JWT_SECRET is still the development placeholder. Set a unique secret before deploying.");
        }
        if (secret.length() < MIN_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_LENGTH + " characters in production.");
        }
    }
}
