package com.agentopscrm.entity.enums;

/**
 * Status of an outbound lead discovery run (Apify Lead Finder, F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public enum LeadSourceRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
