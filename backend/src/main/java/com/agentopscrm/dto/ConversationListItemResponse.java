package com.agentopscrm.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for conversation list items.
 * Optimized for list display without loading all messages.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
public class ConversationListItemResponse {
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
    private int messageCount;
    private int leadCount;
    private String latestMessagePreview;
    private String latestMessageRole;
    private LocalDateTime latestMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConversationListItemResponse() {
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

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getLeadCount() {
        return leadCount;
    }

    public void setLeadCount(int leadCount) {
        this.leadCount = leadCount;
    }

    public String getLatestMessagePreview() {
        return latestMessagePreview;
    }

    public void setLatestMessagePreview(String latestMessagePreview) {
        this.latestMessagePreview = latestMessagePreview;
    }

    public String getLatestMessageRole() {
        return latestMessageRole;
    }

    public void setLatestMessageRole(String latestMessageRole) {
        this.latestMessageRole = latestMessageRole;
    }

    public LocalDateTime getLatestMessageAt() {
        return latestMessageAt;
    }

    public void setLatestMessageAt(LocalDateTime latestMessageAt) {
        this.latestMessageAt = latestMessageAt;
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
}
