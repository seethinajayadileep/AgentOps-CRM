package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public class ApprovalStatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private ApprovalStatus status;

    public ApprovalStatusUpdateRequest() {}

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
}
