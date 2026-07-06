package com.agentopscrm.dto.settings;

import com.agentopscrm.entity.enums.ReadinessStatus;
import java.time.Instant;

/**
 * Integration connection test result.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class IntegrationTestResult {
    private String integration;
    private ReadinessStatus status;
    private boolean success;
    private String message;
    private Long durationMs;
    private Instant testedAt;

    public IntegrationTestResult() {
    }

    public IntegrationTestResult(String integration, boolean success, String message) {
        this.integration = integration;
        this.success = success;
        this.status = success ? ReadinessStatus.HEALTHY : ReadinessStatus.ERROR;
        this.message = message;
        this.testedAt = Instant.now();
    }

    // Getters and setters
    public String getIntegration() {
        return integration;
    }

    public void setIntegration(String integration) {
        this.integration = integration;
    }

    public ReadinessStatus getStatus() {
        return status;
    }

    public void setStatus(ReadinessStatus status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(Instant testedAt) {
        this.testedAt = testedAt;
    }
}
