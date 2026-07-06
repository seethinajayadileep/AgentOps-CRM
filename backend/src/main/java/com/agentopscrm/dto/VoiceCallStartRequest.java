package com.agentopscrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for starting a voice call.
 * 
 * Why exists: Validates and structures the request to initiate a voice call via Vapi.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
public class VoiceCallStartRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String notes;

    public VoiceCallStartRequest() {
    }

    public VoiceCallStartRequest(String phoneNumber, String notes) {
        this.phoneNumber = phoneNumber;
        this.notes = notes;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
