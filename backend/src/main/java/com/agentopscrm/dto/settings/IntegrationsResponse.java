package com.agentopscrm.dto.settings;

import java.util.List;

/**
 * Integrations overview response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class IntegrationsResponse {
    private List<IntegrationStatus> integrations;

    public IntegrationsResponse() {
    }

    public IntegrationsResponse(List<IntegrationStatus> integrations) {
        this.integrations = integrations;
    }

    public List<IntegrationStatus> getIntegrations() {
        return integrations;
    }

    public void setIntegrations(List<IntegrationStatus> integrations) {
        this.integrations = integrations;
    }
}
