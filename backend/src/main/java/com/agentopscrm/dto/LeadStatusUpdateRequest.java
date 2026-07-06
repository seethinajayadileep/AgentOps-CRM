package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating lead status.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class LeadStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private LeadStatus status;

    public LeadStatusUpdateRequest() {
    }

    public LeadStatusUpdateRequest(LeadStatus status) {
        this.status = status;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }
}
