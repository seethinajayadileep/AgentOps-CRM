package com.agentopscrm.entity.enums;

/**
 * Status of a discovered lead (outbound prospect) found via Apify Lead Finder (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public enum DiscoveredLeadStatus {
    NEW,
    REVIEWED,
    IMPORTED,
    REJECTED
}
