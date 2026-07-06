package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.AgentActionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for AgentLog entity.
 *
 * Why exists: Provides detailed agent execution information without exposing JPA entities.
 *
 * @author AgentOps Team
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */
public class AgentLogResponse {
    private UUID id;
    private String agentName;
    private String action;
    private AgentActionStatus status;
    private Long durationMs;
    private LocalDateTime createdAt;
    // Related entity info
    private UUID businessId;
    private String businessName;
    private UUID leadId;
    private String leadName;
    private UUID conversationId;
    // Execution details
    private String inputJson;
    private String outputJson;
    private String errorMessage;

    public AgentLogResponse() {
    }

    // Constructor for easy mapping
    public AgentLogResponse(
        UUID id,
        String agentName,
        String action,
        AgentActionStatus status,
        Long durationMs,
        LocalDateTime createdAt,
        UUID businessId,
        String businessName,
        UUID leadId,
        String leadName,
        UUID conversationId,
        String inputJson,
        String outputJson,
        String errorMessage
    ) {
        this.id = id;
        this.agentName = agentName;
        this.action = action;
        this.status = status;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
        this.businessId = businessId;
        this.businessName = businessName;
        this.leadId = leadId;
        this.leadName = leadName;
        this.conversationId = conversationId;
        this.inputJson = inputJson;
        this.outputJson = outputJson;
        this.errorMessage = errorMessage;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public AgentActionStatus getStatus() {
        return status;
    }

    public void setStatus(AgentActionStatus status) {
        this.status = status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
    }

    public String getLeadName() {
        return leadName;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public void setOutputJson(String outputJson) {
        this.outputJson = outputJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
