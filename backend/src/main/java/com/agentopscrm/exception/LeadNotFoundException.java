package com.agentopscrm.exception;

/**
 * Exception thrown when a lead is not found.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException(String message) {
        super(message);
    }

    public LeadNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
