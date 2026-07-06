package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.ConversationStatus;

/**
 * Request DTO for updating conversation status.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
public class ConversationStatusUpdateRequest {
    private ConversationStatus status;

    public ConversationStatusUpdateRequest() {
    }

    public ConversationStatusUpdateRequest(ConversationStatus status) {
        this.status = status;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }
}
