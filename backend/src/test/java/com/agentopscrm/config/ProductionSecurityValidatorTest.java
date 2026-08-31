package com.agentopscrm.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSecurityValidatorTest {

    @Test
    void rejectsBlankSecret() {
        assertThrows(IllegalStateException.class, () -> ProductionSecurityValidator.validateJwtSecret(""));
        assertThrows(IllegalStateException.class, () -> ProductionSecurityValidator.validateJwtSecret(null));
        assertThrows(IllegalStateException.class, () -> ProductionSecurityValidator.validateJwtSecret("   "));
    }

    @Test
    void rejectsDevelopmentPlaceholder() {
        assertThrows(
                IllegalStateException.class,
                () -> ProductionSecurityValidator.validateJwtSecret(ProductionSecurityValidator.DEV_PLACEHOLDER));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> ProductionSecurityValidator.validateJwtSecret("too-short-to-be-safe"));
    }

    @Test
    void acceptsLongUniqueSecret() {
        assertDoesNotThrow(() -> ProductionSecurityValidator.validateJwtSecret(
                "railway-prod-jwt-secret-value-at-least-32-chars"));
    }
}
