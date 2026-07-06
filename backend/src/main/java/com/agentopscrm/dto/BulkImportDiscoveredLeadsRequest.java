package com.agentopscrm.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO to import multiple discovered leads at once (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class BulkImportDiscoveredLeadsRequest {

    @NotEmpty(message = "discoveredLeadIds must not be empty")
    private List<UUID> discoveredLeadIds;

    private UUID targetBusinessId;

    public BulkImportDiscoveredLeadsRequest() {
    }

    public List<UUID> getDiscoveredLeadIds() {
        return discoveredLeadIds;
    }

    public void setDiscoveredLeadIds(List<UUID> discoveredLeadIds) {
        this.discoveredLeadIds = discoveredLeadIds;
    }

    public UUID getTargetBusinessId() {
        return targetBusinessId;
    }

    public void setTargetBusinessId(UUID targetBusinessId) {
        this.targetBusinessId = targetBusinessId;
    }
}
