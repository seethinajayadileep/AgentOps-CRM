package com.agentopscrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for lead qualification from chat messages.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class LeadQualificationRequest {

    @NotNull(message = "Business ID is required")
    private UUID businessId;

    private UUID conversationId;

    @NotBlank(message = "Message cannot be blank")
    private String message;

    public LeadQualificationRequest() {
    }

    public LeadQualificationRequest(UUID businessId, UUID conversationId, String message) {
        this.businessId = businessId;
        this.conversationId = conversationId;
        this.message = message;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
