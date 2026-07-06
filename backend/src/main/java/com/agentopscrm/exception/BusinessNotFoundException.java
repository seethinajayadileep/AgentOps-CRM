package com.agentopscrm.exception;

/**
 * Exception thrown when a business is not found.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class BusinessNotFoundException extends RuntimeException {

    public BusinessNotFoundException(String message) {
        super(message);
    }

    public BusinessNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}