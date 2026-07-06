package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.LeadStatus;

import java.util.UUID;

/**
 * Response DTO for lead qualification.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class LeadQualificationResponse {

    private UUID leadId;
    private String name;
    private String email;
    private String phone;
    private String requirementText;
    private String budget;
    private String urgency;
    private String timeline;
    private Double leadScore;
    private LeadStatus status;
    private String summary;

    public LeadQualificationResponse() {
    }

    public UUID getLeadId() {
        return leadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
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

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
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

    public Double getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Double leadScore) {
        this.leadScore = leadScore;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
