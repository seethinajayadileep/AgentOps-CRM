package com.agentopscrm.dto;

/**
 * Service status DTO for individual service health.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class ServiceStatus {
    private String status;
    private String message;

    public ServiceStatus() {
    }

    public ServiceStatus(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}