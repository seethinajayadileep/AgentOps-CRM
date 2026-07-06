package com.agentopscrm.dto.settings;

import com.agentopscrm.entity.enums.ReadinessStatus;
import java.time.Instant;

/**
 * Integration status DTO for a single integration.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class IntegrationStatus {
    private String name;
    private String purpose;
    private ReadinessStatus status;
    private boolean configured;
    private boolean enabled;
    private String message;
    private Instant lastChecked;
    private String configDetails;

    public IntegrationStatus() {
    }

    public IntegrationStatus(String name, String purpose, ReadinessStatus status) {
        this.name = name;
        this.purpose = purpose;
        this.status = status;
        this.lastChecked = Instant.now();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public ReadinessStatus getStatus() {
        return status;
    }

    public void setStatus(ReadinessStatus status) {
        this.status = status;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Instant lastChecked) {
        this.lastChecked = lastChecked;
    }

    public String getConfigDetails() {
        return configDetails;
    }

    public void setConfigDetails(String configDetails) {
        this.configDetails = configDetails;
    }
}
