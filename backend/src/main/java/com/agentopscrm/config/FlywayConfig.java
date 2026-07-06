package com.agentopscrm.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway Configuration
 * 
 * Handles special migration requirements:
 * - V10: CREATE INDEX CONCURRENTLY requires executeInTransaction=false
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Custom strategy to handle V10 migration
            // V10 uses CREATE INDEX CONCURRENTLY which cannot run in a transaction
            
            FluentConfiguration configuration = Flyway.configure()
                    .configuration(flyway.getConfiguration())
                    // Mixed mode: allows both transactional and non-transactional migrations
                    .mixed(true);
            
            Flyway customFlyway = configuration.load();
            customFlyway.migrate();
        };
    }
}
