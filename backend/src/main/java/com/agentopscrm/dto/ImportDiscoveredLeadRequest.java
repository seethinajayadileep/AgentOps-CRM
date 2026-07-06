package com.agentopscrm.dto;

import java.util.UUID;

/**
 * Request DTO to import a single discovered lead into the CRM Lead table (F-010).
 *
 * The target business is optional at the DTO level; the service enforces it only when the
 * Lead model requires a business link (which it currently does).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class ImportDiscoveredLeadRequest {

    private UUID targetBusinessId;

    public ImportDiscoveredLeadRequest() {
    }

    public UUID getTargetBusinessId() {
        return targetBusinessId;
    }

    public void setTargetBusinessId(UUID targetBusinessId) {
        this.targetBusinessId = targetBusinessId;
    }
}
