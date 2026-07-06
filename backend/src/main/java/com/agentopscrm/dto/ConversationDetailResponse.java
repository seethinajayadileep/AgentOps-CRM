package com.agentopscrm.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for conversation details.
 * Includes full conversation information and related entity summaries.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
public class ConversationDetailResponse {
    private UUID id;
    private UUID businessId;
    private String businessName;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String channel;
    private String status;
    private String summary;
    private String leadCaptureStatus;
    private String pendingLeadName;
    private String pendingLeadEmail;
    private String pendingLeadPhone;
    private String pendingLeadRequirement;
    private List<RelatedLead> relatedLeads;
    private int voiceCallCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConversationDetailResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public List<RelatedLead> getRelatedLeads() {
        return relatedLeads;
    }

    public void setRelatedLeads(List<RelatedLead> relatedLeads) {
        this.relatedLeads = relatedLeads;
    }

    public int getVoiceCallCount() {
        return voiceCallCount;
    }

    public void setVoiceCallCount(int voiceCallCount) {
        this.voiceCallCount = voiceCallCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Nested DTO for related leads (minimal info to avoid recursive loading)
     */
    public static class RelatedLead {
        private UUID id;
        private String name;
        private String email;
        private String status;
        private Integer leadScore;

        public RelatedLead() {
        }

        public RelatedLead(UUID id, String name, String email, String status, Integer leadScore) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.status = status;
            this.leadScore = leadScore;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getLeadScore() {
            return leadScore;
        }

        public void setLeadScore(Integer leadScore) {
            this.leadScore = leadScore;
        }
    }
}
