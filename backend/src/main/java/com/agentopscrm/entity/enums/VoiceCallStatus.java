package com.agentopscrm.entity.enums;

/**
 * Status of a voice call.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public enum VoiceCallStatus {
    PENDING,      // Call created, not yet initiated
    SCHEDULED,    // Call scheduled for future
    STARTED,      // Call initiated/ringing
    IN_PROGRESS,  // Call is active
    COMPLETED,    // Call completed successfully
    FAILED,       // Call failed to connect
    NO_ANSWER,    // Call rang but was not answered
    BUSY,         // Line was busy
    VOICEMAIL,    // Reached voicemail
    CANCELLED     // Call was cancelled
}