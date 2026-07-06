package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.CallOutcome;
import com.agentopscrm.entity.enums.VoiceCallStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * VoiceCall entity representing a voice call made through Vapi.
 *
 * Why exists: Tracks voice call details including transcript, summary,
 * and link to the external Vapi call.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "voice_calls", indexes = {
    @Index(name = "idx_voice_calls_business_id", columnList = "business_id"),
    @Index(name = "idx_voice_calls_lead_id", columnList = "lead_id"),
    @Index(name = "idx_voice_calls_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_voice_calls_status", columnList = "status"),
    @Index(name = "idx_voice_calls_started_at", columnList = "started_at")
})
public class VoiceCall extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "vapi_call_id", length = 255)
    private String vapiCallId;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoiceCallStatus status = VoiceCallStatus.PENDING;

    @Lob
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "recording_url", length = 1000)
    private String recordingUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "provider", length = 50)
    private String provider = "vapi";

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 50)
    private CallOutcome outcome;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    public VoiceCall() {
        super();
    }

    public VoiceCall(UUID id) {
        super();
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getVapiCallId() {
        return vapiCallId;
    }

    public void setVapiCallId(String vapiCallId) {
        this.vapiCallId = vapiCallId;
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
}