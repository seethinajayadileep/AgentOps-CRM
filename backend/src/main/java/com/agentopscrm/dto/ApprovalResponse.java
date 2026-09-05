package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApprovalResponse {
    
    private UUID approvalId;
    private ApprovalType type;
    private ApprovalStatus status;
    private String style; // PROFESSIONAL, FRIENDLY, SHORT_WHATSAPP
    private String content;
    private UUID leadId;
    private String leadName;
    private UUID businessId;
    private String businessName;
    private String leadEmail;
    private String sentTo;
    private String sentSubject;
    private String resendMessageId;
    private LocalDateTime sentAt;
    private String sendError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ApprovalResponse() {}

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public UUID getApprovalId() { return approvalId; }
    public ApprovalType getType() { return type; }
    public ApprovalStatus getStatus() { return status; }
    public String getStyle() { return style; }
    public String getContent() { return content; }
    public UUID getLeadId() { return leadId; }
    public String getLeadName() { return leadName; }
    public UUID getBusinessId() { return businessId; }
    public String getBusinessName() { return businessName; }
    public String getLeadEmail() { return leadEmail; }
    public String getSentTo() { return sentTo; }
    public String getSentSubject() { return sentSubject; }
    public String getResendMessageId() { return resendMessageId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getSendError() { return sendError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setApprovalId(UUID approvalId) { this.approvalId = approvalId; }
    public void setType(ApprovalType type) { this.type = type; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
    public void setStyle(String style) { this.style = style; }
    public void setContent(String content) { this.content = content; }
    public void setLeadId(UUID leadId) { this.leadId = leadId; }
    public void setLeadName(String leadName) { this.leadName = leadName; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setLeadEmail(String leadEmail) { this.leadEmail = leadEmail; }
    public void setSentTo(String sentTo) { this.sentTo = sentTo; }
    public void setSentSubject(String sentSubject) { this.sentSubject = sentSubject; }
    public void setResendMessageId(String resendMessageId) { this.resendMessageId = resendMessageId; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public void setSendError(String sendError) { this.sendError = sendError; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private ApprovalResponse response = new ApprovalResponse();

        public Builder approvalId(UUID approvalId) { response.approvalId = approvalId; return this; }
        public Builder type(ApprovalType type) { response.type = type; return this; }
        public Builder status(ApprovalStatus status) { response.status = status; return this; }
        public Builder style(String style) { response.style = style; return this; }
        public Builder content(String content) { response.content = content; return this; }
        public Builder leadId(UUID leadId) { response.leadId = leadId; return this; }
        public Builder leadName(String leadName) { response.leadName = leadName; return this; }
        public Builder businessId(UUID businessId) { response.businessId = businessId; return this; }
        public Builder businessName(String businessName) { response.businessName = businessName; return this; }
        public Builder leadEmail(String leadEmail) { response.leadEmail = leadEmail; return this; }
        public Builder sentTo(String sentTo) { response.sentTo = sentTo; return this; }
        public Builder sentSubject(String sentSubject) { response.sentSubject = sentSubject; return this; }
        public Builder resendMessageId(String resendMessageId) { response.resendMessageId = resendMessageId; return this; }
        public Builder sentAt(LocalDateTime sentAt) { response.sentAt = sentAt; return this; }
        public Builder sendError(String sendError) { response.sendError = sendError; return this; }
        public Builder createdAt(LocalDateTime createdAt) { response.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { response.updatedAt = updatedAt; return this; }

        public ApprovalResponse build() { return response; }
    }
}
