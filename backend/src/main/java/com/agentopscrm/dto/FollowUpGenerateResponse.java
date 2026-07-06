package com.agentopscrm.dto;

import java.util.List;
import java.util.UUID;

public class FollowUpGenerateResponse {
    
    private UUID leadId;
    private List<ApprovalResponse> approvals;

    public FollowUpGenerateResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public UUID getLeadId() { return leadId; }
    public List<ApprovalResponse> getApprovals() { return approvals; }

    public void setLeadId(UUID leadId) { this.leadId = leadId; }
    public void setApprovals(List<ApprovalResponse> approvals) { this.approvals = approvals; }

    public static class Builder {
        private FollowUpGenerateResponse response = new FollowUpGenerateResponse();

        public Builder leadId(UUID leadId) { response.leadId = leadId; return this; }
        public Builder approvals(List<ApprovalResponse> approvals) { response.approvals = approvals; return this; }

        public FollowUpGenerateResponse build() { return response; }
    }
}
