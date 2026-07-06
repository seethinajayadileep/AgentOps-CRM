package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.CrawlStatus;

/**
 * Response DTO for business details.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class BusinessResponse {
    private String id;
    private String name;
    private String websiteUrl;
    private String industry;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String crawlStatus;
    private String createdAt;
    private String updatedAt;

    public BusinessResponse() {
    }

    public BusinessResponse(String id, String name, String websiteUrl, String industry,
                           String description, String contactEmail, String contactPhone,
                           String crawlStatus, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.websiteUrl = websiteUrl;
        this.industry = industry;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.crawlStatus = crawlStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getCrawlStatus() {
        return crawlStatus;
    }

    public void setCrawlStatus(String crawlStatus) {
        this.crawlStatus = crawlStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}