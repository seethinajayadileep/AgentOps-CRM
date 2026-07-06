package com.agentopscrm.entity.enums;

/**
 * Readiness status for integrations and system components.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public enum ReadinessStatus {
    /** Component is fully operational and healthy */
    HEALTHY,
    
    /** Component is configured but not verified as operational */
    CONFIGURED,
    
    /** Required configuration is missing */
    NOT_CONFIGURED,
    
    /** Component is disabled via configuration */
    DISABLED,
    
    /** Component is partially functional or experiencing issues */
    DEGRADED,
    
    /** Component is not functioning */
    ERROR,
    
    /** Component status is unknown or unable to determine */
    UNKNOWN
}
