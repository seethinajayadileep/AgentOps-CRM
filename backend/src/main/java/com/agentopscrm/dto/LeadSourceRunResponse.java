package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.LeadSourceRunStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a LeadSourceRun (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class LeadSourceRunResponse {

    private UUID id;
    private String searchName;
    private String industry;
    private String location;
    private String keywords;
    private String actorId;
    private String apifyRunId;
    private Integer maxResults;
    private LeadSourceRunStatus status;
    private Integer totalResults;
    private Integer importedCount;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LeadSourceRunResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getApifyRunId() {
        return apifyRunId;
    }

    public void setApifyRunId(String apifyRunId) {
        this.apifyRunId = apifyRunId;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public LeadSourceRunStatus getStatus() {
        return status;
    }

    public void setStatus(LeadSourceRunStatus status) {
        this.status = status;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public Integer getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(Integer importedCount) {
        this.importedCount = importedCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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
