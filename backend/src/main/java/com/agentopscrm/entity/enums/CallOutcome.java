package com.agentopscrm.entity.enums;

/**
 * Outcome of a voice call conversation.
 * Used to track the result of the AI voice agent's interaction with the lead.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
public enum CallOutcome {
    /**
     * Lead expressed interest and wants to proceed.
     */
    INTERESTED,
    
    /**
     * Lead is not interested in the offering.
     */
    NOT_INTERESTED,
    
    /**
     * Lead requested a callback from a human agent.
     */
    HUMAN_CALLBACK_REQUESTED,
    
    /**
     * Call went to voicemail and a message was left.
     */
    VOICEMAIL_LEFT,
    
    /**
     * No answer from the lead.
     */
    NO_ANSWER,
    
    /**
     * Call was disconnected or dropped.
     */
    CALL_DISCONNECTED,
    
    /**
     * Lead answered the call and chatted.
     */
    ANSWERED,
    
    /**
     * Lead's phone was busy.
     */
    BUSY,
    
    /**
     * Call reached voicemail (generic).
     */
    VOICEMAIL,
    
    /**
     * Call failed due to technical error.
     */
    FAILED,
    
    /**
     * Call was cancelled.
     */
    CANCELLED,
    
    /**
     * Outcome could not be determined.
     */
    UNKNOWN
}
