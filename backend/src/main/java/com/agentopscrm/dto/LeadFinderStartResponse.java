package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.LeadSourceRunStatus;

import java.util.UUID;

/**
 * Response DTO returned when a lead discovery run is started (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class LeadFinderStartResponse {

    private UUID runId;
    private LeadSourceRunStatus status;
    private String message;

    public LeadFinderStartResponse() {
    }

    public LeadFinderStartResponse(UUID runId, LeadSourceRunStatus status, String message) {
        this.runId = runId;
        this.status = status;
        this.message = message;
    }

    public UUID getRunId() {
        return runId;
    }

    public void setRunId(UUID runId) {
        this.runId = runId;
    }

    public LeadSourceRunStatus getStatus() {
        return status;
    }

    public void setStatus(LeadSourceRunStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
