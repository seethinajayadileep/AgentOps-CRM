package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.LeadSourceRunStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LeadSourceRun entity representing a single outbound lead discovery run via Apify (F-010).
 *
 * Why exists: Tracks each Apify actor search execution and its lifecycle so the CRM can
 * display run history, sync results, and audit outbound lead generation.
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@Entity
@Table(name = "lead_source_runs", indexes = {
    @Index(name = "idx_lead_source_runs_status", columnList = "status"),
    @Index(name = "idx_lead_source_runs_apify_run_id", columnList = "apify_run_id"),
    @Index(name = "idx_lead_source_runs_created_at", columnList = "created_at")
})
public class LeadSourceRun extends AuditableEntity {

    @Column(name = "search_name", length = 255)
    private String searchName;

    @Column(name = "industry", length = 255)
    private String industry;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    @Column(name = "apify_run_id", length = 255)
    private String apifyRunId;

    @Column(name = "apify_dataset_id", length = 255)
    private String apifyDatasetId;

    @Column(name = "max_results")
    private Integer maxResults;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeadSourceRunStatus status = LeadSourceRunStatus.PENDING;

    @Column(name = "total_results", nullable = false)
    private Integer totalResults = 0;

    @Column(name = "imported_count", nullable = false)
    private Integer importedCount = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "last_synced_at")
    private java.time.LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "leadSourceRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscoveredLead> discoveredLeads = new ArrayList<>();

    public LeadSourceRun() {
        super();
    }

    public LeadSourceRun(UUID id) {
        super();
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

    public String getApifyDatasetId() {
        return apifyDatasetId;
    }

    public void setApifyDatasetId(String apifyDatasetId) {
        this.apifyDatasetId = apifyDatasetId;
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

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public java.time.LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(java.time.LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public List<DiscoveredLead> getDiscoveredLeads() {
        return discoveredLeads;
    }

    public void setDiscoveredLeads(List<DiscoveredLead> discoveredLeads) {
        this.discoveredLeads = discoveredLeads;
    }
}
