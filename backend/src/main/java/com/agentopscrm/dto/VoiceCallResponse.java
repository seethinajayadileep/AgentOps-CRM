package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.CallOutcome;
import com.agentopscrm.entity.enums.VoiceCallStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for voice call information.
 * 
 * Why exists: Presents voice call data to clients, including status, outcome, and call details.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
public class VoiceCallResponse {

    private UUID id;
    private UUID leadId;
    private String leadName;
    private String phoneNumber;
    private VoiceCallStatus status;
    private String provider;
    private CallOutcome outcome;
    private String failureReason;
    private String vapiCallId;
    private String transcript;
    private String summary;
    private String recordingUrl;
    private Integer durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;

    public VoiceCallResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
    }

    public String getLeadName() {
        return leadName;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public VoiceCallStatus getStatus() {
        return status;
    }

    public void setStatus(VoiceCallStatus status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public CallOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(CallOutcome outcome) {
        this.outcome = outcome;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getVapiCallId() {
        return vapiCallId;
    }

    public void setVapiCallId(String vapiCallId) {
        this.vapiCallId = vapiCallId;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Builder for VoiceCallResponse.
     */
    public static class Builder {
        private final VoiceCallResponse response;

        public Builder() {
            this.response = new VoiceCallResponse();
        }

        public Builder id(UUID id) {
            response.id = id;
            return this;
        }

        public Builder leadId(UUID leadId) {
            response.leadId = leadId;
            return this;
        }

        public Builder leadName(String leadName) {
            response.leadName = leadName;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            response.phoneNumber = phoneNumber;
            return this;
        }

        public Builder status(VoiceCallStatus status) {
            response.status = status;
            return this;
        }

        public Builder provider(String provider) {
            response.provider = provider;
            return this;
        }

        public Builder outcome(CallOutcome outcome) {
            response.outcome = outcome;
            return this;
        }

        public Builder failureReason(String failureReason) {
            response.failureReason = failureReason;
            return this;
        }

        public Builder vapiCallId(String vapiCallId) {
            response.vapiCallId = vapiCallId;
            return this;
        }

        public Builder transcript(String transcript) {
            response.transcript = transcript;
            return this;
        }

        public Builder summary(String summary) {
            response.summary = summary;
            return this;
        }

        public Builder recordingUrl(String recordingUrl) {
            response.recordingUrl = recordingUrl;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            response.durationSeconds = durationSeconds;
            return this;
        }

        public Builder startedAt(LocalDateTime startedAt) {
            response.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(LocalDateTime endedAt) {
            response.endedAt = endedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            response.createdAt = createdAt;
            return this;
        }

        public VoiceCallResponse build() {
            return response;
        }
    }
}
