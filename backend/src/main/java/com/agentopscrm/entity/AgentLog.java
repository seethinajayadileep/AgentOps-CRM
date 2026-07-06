package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.AgentActionStatus;
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
import java.util.UUID;

/**
 * AgentLog entity representing an agent action execution.
 *
 * Why exists: Complete audit trail of all AI agent actions for transparency,
 * debugging, and compliance tracking.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "agent_logs", indexes = {
    @Index(name = "idx_agent_logs_business_id", columnList = "business_id"),
    @Index(name = "idx_agent_logs_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_agent_logs_lead_id", columnList = "lead_id"),
    @Index(name = "idx_agent_logs_agent_name", columnList = "agent_name"),
    @Index(name = "idx_agent_logs_status", columnList = "status"),
    @Index(name = "idx_agent_logs_created_at", columnList = "created_at")
})
public class AgentLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Lob
    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Lob
    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentActionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    public AgentLog() {
        super();
    }

    public AgentLog(UUID id) {
        super();
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public void setOutputJson(String outputJson) {
        this.outputJson = outputJson;
    }

    public AgentActionStatus getStatus() {
        return status;
    }

    public void setStatus(AgentActionStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}