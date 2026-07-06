package com.agentopscrm.dto.settings;

import com.agentopscrm.entity.enums.ReadinessStatus;
import java.time.Instant;

/**
 * Voice AI (Vapi) configuration and status response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class VoiceConfigResponse {
    private boolean enabled;
    private boolean apiKeyConfigured;
    private boolean assistantIdConfigured;
    private boolean phoneNumberIdConfigured;
    private boolean webhookSecretConfigured;
    private String webhookEndpoint;
    private String webhookUrl;
    private ReadinessStatus status;
    private String statusMessage;
    private boolean metricsAvailable;
    private String metricsMessage;
    private Long totalCalls;
    private Long successfulCalls;
    private Long failedCalls;
    private Instant lastSuccessfulCall;
    private Instant lastFailedCall;

    public VoiceConfigResponse() {
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isApiKeyConfigured() {
        return apiKeyConfigured;
    }

    public void setApiKeyConfigured(boolean apiKeyConfigured) {
        this.apiKeyConfigured = apiKeyConfigured;
    }

    public boolean isAssistantIdConfigured() {
        return assistantIdConfigured;
    }

    public void setAssistantIdConfigured(boolean assistantIdConfigured) {
        this.assistantIdConfigured = assistantIdConfigured;
    }

    public boolean isPhoneNumberIdConfigured() {
        return phoneNumberIdConfigured;
    }

    public void setPhoneNumberIdConfigured(boolean phoneNumberIdConfigured) {
        this.phoneNumberIdConfigured = phoneNumberIdConfigured;
    }

    public boolean isWebhookSecretConfigured() {
        return webhookSecretConfigured;
    }

    public void setWebhookSecretConfigured(boolean webhookSecretConfigured) {
        this.webhookSecretConfigured = webhookSecretConfigured;
    }

    public String getWebhookEndpoint() {
        return webhookEndpoint;
    }

    public void setWebhookEndpoint(String webhookEndpoint) {
        this.webhookEndpoint = webhookEndpoint;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
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

    public boolean isMetricsAvailable() {
        return metricsAvailable;
    }

    public void setMetricsAvailable(boolean metricsAvailable) {
        this.metricsAvailable = metricsAvailable;
    }

    public String getMetricsMessage() {
        return metricsMessage;
    }

    public void setMetricsMessage(String metricsMessage) {
        this.metricsMessage = metricsMessage;
    }

    public Long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public Long getSuccessfulCalls() {
        return successfulCalls;
    }

    public void setSuccessfulCalls(Long successfulCalls) {
        this.successfulCalls = successfulCalls;
    }

    public Long getFailedCalls() {
        return failedCalls;
    }

    public void setFailedCalls(Long failedCalls) {
        this.failedCalls = failedCalls;
    }

    public Instant getLastSuccessfulCall() {
        return lastSuccessfulCall;
    }

    public void setLastSuccessfulCall(Instant lastSuccessfulCall) {
        this.lastSuccessfulCall = lastSuccessfulCall;
    }

    public Instant getLastFailedCall() {
        return lastFailedCall;
    }

    public void setLastFailedCall(Instant lastFailedCall) {
        this.lastFailedCall = lastFailedCall;
    }
}
