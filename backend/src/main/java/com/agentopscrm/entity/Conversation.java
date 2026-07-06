package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.Channel;
import com.agentopscrm.entity.enums.ConversationStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conversation entity representing a customer conversation.
 *
 * Why exists: Tracks ongoing conversations with customers across different channels.
 * Central entity linking messages, leads, and voice calls.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversations_business_id", columnList = "business_id"),
    @Index(name = "idx_conversations_customer_email", columnList = "customer_email"),
    @Index(name = "idx_conversations_status", columnList = "status")
})
public class Conversation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "lead_capture_status", length = 50)
    private String leadCaptureStatus; // null, AWAITING_DETAILS, COLLECTING_DETAILS, CAPTURED

    @Column(name = "pending_lead_name", length = 255)
    private String pendingLeadName;

    @Column(name = "pending_lead_email", length = 255)
    private String pendingLeadEmail;

    @Column(name = "pending_lead_phone", length = 50)
    private String pendingLeadPhone;

    @Column(name = "pending_lead_requirement", columnDefinition = "TEXT")
    private String pendingLeadRequirement;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<Lead> leads = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<VoiceCall> voiceCalls = new ArrayList<>();

    public Conversation() {
        super();
    }

    public Conversation(UUID id) {
        super();
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLeadCaptureStatus() {
        return leadCaptureStatus;
    }

    public void setLeadCaptureStatus(String leadCaptureStatus) {
        this.leadCaptureStatus = leadCaptureStatus;
    }

    public String getPendingLeadName() {
        return pendingLeadName;
    }

    public void setPendingLeadName(String pendingLeadName) {
        this.pendingLeadName = pendingLeadName;
    }

    public String getPendingLeadEmail() {
        return pendingLeadEmail;
    }

    public void setPendingLeadEmail(String pendingLeadEmail) {
        this.pendingLeadEmail = pendingLeadEmail;
    }

    public String getPendingLeadPhone() {
        return pendingLeadPhone;
    }

    public void setPendingLeadPhone(String pendingLeadPhone) {
        this.pendingLeadPhone = pendingLeadPhone;
    }

    public String getPendingLeadRequirement() {
        return pendingLeadRequirement;
    }

    public void setPendingLeadRequirement(String pendingLeadRequirement) {
        this.pendingLeadRequirement = pendingLeadRequirement;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }

    public List<VoiceCall> getVoiceCalls() {
        return voiceCalls;
    }

    public void setVoiceCalls(List<VoiceCall> voiceCalls) {
        this.voiceCalls = voiceCalls;
    }
}