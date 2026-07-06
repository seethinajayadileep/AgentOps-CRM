package com.agentopscrm.dto.settings;

import java.util.List;

/**
 * System diagnostics and configuration response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class SystemDiagnosticsResponse {
    private String applicationName;
    private String applicationVersion;
    private String backendVersion;
    private String activeProfile;
    private String apiBasePath;
    private String serverTimezone;
    private String databaseType;
    private boolean redisConfigured;
    private boolean flywayEnabled;
    private String hibernateSchemaMode;
    private String vectorStoreStrategy;
    private List<SystemWarning> warnings;

    public SystemDiagnosticsResponse() {
    }

    // Getters and setters
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getBackendVersion() {
        return backendVersion;
    }

    public void setBackendVersion(String backendVersion) {
        this.backendVersion = backendVersion;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String activeProfile) {
        this.activeProfile = activeProfile;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public String getServerTimezone() {
        return serverTimezone;
    }

    public void setServerTimezone(String serverTimezone) {
        this.serverTimezone = serverTimezone;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public boolean isRedisConfigured() {
        return redisConfigured;
    }

    public void setRedisConfigured(boolean redisConfigured) {
        this.redisConfigured = redisConfigured;
    }

    public boolean isFlywayEnabled() {
        return flywayEnabled;
    }

    public void setFlywayEnabled(boolean flywayEnabled) {
        this.flywayEnabled = flywayEnabled;
    }

    public String getHibernateSchemaMode() {
        return hibernateSchemaMode;
    }

    public void setHibernateSchemaMode(String hibernateSchemaMode) {
        this.hibernateSchemaMode = hibernateSchemaMode;
    }

    public String getVectorStoreStrategy() {
        return vectorStoreStrategy;
    }

    public void setVectorStoreStrategy(String vectorStoreStrategy) {
        this.vectorStoreStrategy = vectorStoreStrategy;
    }

    public List<SystemWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<SystemWarning> warnings) {
        this.warnings = warnings;
    }

    public static class SystemWarning {
        private String type;
        private String message;
        private String recommendation;

        public SystemWarning() {
        }

        public SystemWarning(String type, String message, String recommendation) {
            this.type = type;
            this.message = message;
            this.recommendation = recommendation;
        }

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
    }
}
