package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.LeadStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lead entity representing a qualified potential customer.
 *
 * Why exists: Central entity for CRM - tracks qualified leads from conversations
 * with scoring, requirements, and follow-up status.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_business_id", columnList = "business_id"),
    @Index(name = "idx_leads_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_leads_status", columnList = "status"),
    @Index(name = "idx_leads_lead_score", columnList = "lead_score"),
    @Index(name = "idx_leads_email", columnList = "email")
})
public class Lead extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "requirement_text", columnDefinition = "TEXT")
    private String requirementText;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "urgency", length = 20)
    private String urgency;

    @Column(name = "timeline", length = 50)
    private String timeline;

    @Column(name = "lead_score", precision = 5, scale = 2)
    private BigDecimal leadScore;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeadStatus status = LeadStatus.NEW;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL)
    private List<Approval> approvals = new ArrayList<>();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL)
    private List<VoiceCall> voiceCalls = new ArrayList<>();

    public Lead() {
        super();
    }

    public Lead(UUID id) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public void setRequirementText(String requirementText) {
        this.requirementText = requirementText;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getTimeline() {
        return timeline;
    }

    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }

    public BigDecimal getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(BigDecimal leadScore) {
        this.leadScore = leadScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public List<Approval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<Approval> approvals) {
        this.approvals = approvals;
    }

    public List<VoiceCall> getVoiceCalls() {
        return voiceCalls;
    }

    public void setVoiceCalls(List<VoiceCall> voiceCalls) {
        this.voiceCalls = voiceCalls;
    }
}