package com.agentopscrm.dto;

import java.util.Map;

/**
 * Response DTO for health check endpoint.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class HealthResponse {
    private String status;
    private String timestamp;
    private Map<String, ServiceStatus> services;
    private String version;

    public HealthResponse() {
    }

    public HealthResponse(String status, String timestamp, Map<String, ServiceStatus> services, String version) {
        this.status = status;
        this.timestamp = timestamp;
        this.services = services;
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, ServiceStatus> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceStatus> services) {
        this.services = services;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}