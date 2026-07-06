package com.agentopscrm.dto.settings;

import com.agentopscrm.entity.enums.ReadinessStatus;
import java.time.Instant;
import java.util.Map;

/**
 * System health overview response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class SystemHealthResponse {
    private String applicationName;
    private String applicationVersion;
    private String activeProfile;
    private String environment;
    private Instant serverTime;
    private Instant lastHealthCheck;
    private Map<String, ReadinessStatus> components;

    public SystemHealthResponse() {
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

    public String getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String activeProfile) {
        this.activeProfile = activeProfile;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Instant getServerTime() {
        return serverTime;
    }

    public void setServerTime(Instant serverTime) {
        this.serverTime = serverTime;
    }

    public Instant getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(Instant lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }

    public Map<String, ReadinessStatus> getComponents() {
        return components;
    }

    public void setComponents(Map<String, ReadinessStatus> components) {
        this.components = components;
    }
}
