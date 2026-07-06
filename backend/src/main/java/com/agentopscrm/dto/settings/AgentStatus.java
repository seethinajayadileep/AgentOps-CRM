package com.agentopscrm.dto.settings;

import com.agentopscrm.entity.enums.ReadinessStatus;

/**
 * Agent readiness status DTO.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class AgentStatus {
    private String agentName;
    private ReadinessStatus status;
    private String statusMessage;
    private String requiredIntegration;
    private String currentModel;
    private boolean fallbackAvailable;

    public AgentStatus() {
    }

    public AgentStatus(String agentName, ReadinessStatus status, String statusMessage) {
        this.agentName = agentName;
        this.status = status;
        this.statusMessage = statusMessage;
    }

    // Getters and setters
    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public ReadinessStatus getStatus() {
        return status;
    }

    public void setStatus(ReadinessStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getRequiredIntegration() {
        return requiredIntegration;
    }

    public void setRequiredIntegration(String requiredIntegration) {
        this.requiredIntegration = requiredIntegration;
    }

    public String getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public boolean isFallbackAvailable() {
        return fallbackAvailable;
    }

    public void setFallbackAvailable(boolean fallbackAvailable) {
        this.fallbackAvailable = fallbackAvailable;
    }
}
