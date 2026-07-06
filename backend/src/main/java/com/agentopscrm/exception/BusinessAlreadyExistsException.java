package com.agentopscrm.exception;

/**
 * Exception thrown when trying to create a business with a duplicate website URL.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class BusinessAlreadyExistsException extends RuntimeException {

    public BusinessAlreadyExistsException(String message) {
        super(message);
    }

    public BusinessAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}