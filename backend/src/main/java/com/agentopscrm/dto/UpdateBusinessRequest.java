package com.agentopscrm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing business.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class UpdateBusinessRequest {

    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String name;

    @Pattern(regexp = "^https?://.+", message = "Website URL must start with http:// or https://")
    private String websiteUrl;

    @Size(max = 100, message = "Industry must be at most 100 characters")
    private String industry;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Pattern(regexp = "^$|^\\+?[0-9\\-\\s\\(\\)]{7,20}$", message = "Invalid phone format")
    private String contactPhone;

    public UpdateBusinessRequest() {
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
}