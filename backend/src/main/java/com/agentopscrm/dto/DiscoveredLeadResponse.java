package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.DiscoveredLeadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a DiscoveredLead (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class DiscoveredLeadResponse {

    private UUID id;
    private UUID leadSourceRunId;
    private String businessName;
    private String websiteUrl;
    private String contactName;
    private String email;
    private String phone;
    private String location;
    private String industry;
    private String sourceUrl;
    private String rawDataJson;
    private Double score;
    private DiscoveredLeadStatus status;
    private UUID importedLeadId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DiscoveredLeadResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLeadSourceRunId() {
        return leadSourceRunId;
    }

    public void setLeadSourceRunId(UUID leadSourceRunId) {
        this.leadSourceRunId = leadSourceRunId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getRawDataJson() {
        return rawDataJson;
    }

    public void setRawDataJson(String rawDataJson) {
        this.rawDataJson = rawDataJson;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public DiscoveredLeadStatus getStatus() {
        return status;
    }

    public void setStatus(DiscoveredLeadStatus status) {
        this.status = status;
    }

    public UUID getImportedLeadId() {
        return importedLeadId;
    }

    public void setImportedLeadId(UUID importedLeadId) {
        this.importedLeadId = importedLeadId;
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
