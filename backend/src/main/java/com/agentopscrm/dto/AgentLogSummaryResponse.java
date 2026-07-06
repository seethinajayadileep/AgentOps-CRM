package com.agentopscrm.dto;

/**
 * Response DTO for agent logs summary statistics.
 *
 * Why exists: Provides high-level observability metrics for agent execution health.
 *
 * @author AgentOps Team
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */
public class AgentLogSummaryResponse {
    private long executionsToday;
    private double successRate;
    private long errorCount;
    private Long averageDurationMs;

    public AgentLogSummaryResponse() {
    }

    public AgentLogSummaryResponse(long executionsToday, double successRate, long errorCount, Long averageDurationMs) {
        this.executionsToday = executionsToday;
        this.successRate = successRate;
        this.errorCount = errorCount;
        this.averageDurationMs = averageDurationMs;
    }

    public long getExecutionsToday() {
        return executionsToday;
    }

    public void setExecutionsToday(long executionsToday) {
        this.executionsToday = executionsToday;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public Long getAverageDurationMs() {
        return averageDurationMs;
    }

    public void setAverageDurationMs(Long averageDurationMs) {
        this.averageDurationMs = averageDurationMs;
    }
}
