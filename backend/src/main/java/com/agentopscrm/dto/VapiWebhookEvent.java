package com.agentopscrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a webhook event from Vapi.
 * 
 * Why exists: Captures webhook payloads sent by Vapi to notify about call status changes.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VapiWebhookEvent {

    @JsonProperty("type")
    private String type;

    @JsonProperty("call")
    private CallData call;

    @JsonProperty("message")
    private MessageData message;

    public VapiWebhookEvent() {
    }

    public String getType() {
        if (type != null) {
            return type;
        }
        // Fall back to message.type if top-level type is not present
        return message != null ? message.getType() : null;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CallData getCall() {
        if (call != null) {
            return call;
        }
        // Fall back to message.call if top-level call is not present
        return message != null ? message.getCall() : null;
    }

    public void setCall(CallData call) {
        this.call = call;
    }

    public MessageData getMessage() {
        return message;
    }

    public void setMessage(MessageData message) {
        this.message = message;
    }

    /**
     * Nested message data from webhook (for newer Vapi webhook format).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageData {
        @JsonProperty("type")
        private String type;

        // Vapi "status-update" / "end-of-call-report" events carry the call
        // status here on the message (e.g. "ended"), NOT on a nested call
        // object. Without this the call status was never updated and stayed
        // stuck at PENDING.
        @JsonProperty("status")
        private String status;

        @JsonProperty("call")
        private CallData call;

        @JsonProperty("timestamp")
        private Long timestamp;

        @JsonProperty("artifact")
        private ArtifactData artifact;

        @JsonProperty("startedAt")
        private String startedAt;

        @JsonProperty("endedAt")
        private String endedAt;

        @JsonProperty("endedReason")
        private String endedReason;

        @JsonProperty("transcript")
        private String transcript;

        @JsonProperty("durationSeconds")
        private Integer durationSeconds;

        @JsonProperty("summary")
        private String summary;

        public MessageData() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public CallData getCall() {
            return call;
        }

        public void setCall(CallData call) {
            this.call = call;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public ArtifactData getArtifact() {
            return artifact;
        }

        public void setArtifact(ArtifactData artifact) {
            this.artifact = artifact;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(String startedAt) {
            this.startedAt = startedAt;
        }

        public String getEndedAt() {
            return endedAt;
        }

        public void setEndedAt(String endedAt) {
            this.endedAt = endedAt;
        }

        public String getEndedReason() {
            return endedReason;
        }

        public void setEndedReason(String endedReason) {
            this.endedReason = endedReason;
        }

        public String getTranscript() {
            return transcript;
        }

        public void setTranscript(String transcript) {
            this.transcript = transcript;
        }

        public Integer getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }

    /**
     * Nested artifact data from end-of-call-report webhook.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArtifactData {
        @JsonProperty("transcript")
        private String transcript;

        @JsonProperty("recordingUrl")
        private String recordingUrl;

        @JsonProperty("summary")
        private String summary;

        public ArtifactData() {
        }

        public String getTranscript() {
            return transcript;
        }

        public void setTranscript(String transcript) {
            this.transcript = transcript;
        }

        public String getRecordingUrl() {
            return recordingUrl;
        }

        public void setRecordingUrl(String recordingUrl) {
            this.recordingUrl = recordingUrl;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }

    /**
     * Nested call data from webhook.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("transcript")
        private String transcript;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("recordingUrl")
        private String recordingUrl;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("startedAt")
        private String startedAt;

        @JsonProperty("endedAt")
        private String endedAt;

        @JsonProperty("endedReason")
        private String endedReason;

        @JsonProperty("error")
        private String error;

        public CallData() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
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

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(String startedAt) {
            this.startedAt = startedAt;
        }

        public String getEndedAt() {
            return endedAt;
        }

        public void setEndedAt(String endedAt) {
            this.endedAt = endedAt;
        }

        public String getEndedReason() {
            return endedReason;
        }

        public void setEndedReason(String endedReason) {
            this.endedReason = endedReason;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
