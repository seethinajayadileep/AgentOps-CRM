package com.agentopscrm.dto;

import jakarta.validation.constraints.NotBlank;

public class FollowUpGenerateRequest {
    
    @NotBlank(message = "Tone is required")
    private String tone; // "ALL", "PROFESSIONAL", "FRIENDLY", "SHORT_WHATSAPP"

    public FollowUpGenerateRequest() {}

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
}
