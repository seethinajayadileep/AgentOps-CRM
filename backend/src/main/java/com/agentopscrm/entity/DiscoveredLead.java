package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.DiscoveredLeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * DiscoveredLead entity representing a single prospect found during an Apify lead
 * discovery run (F-010). Normalized shape so the underlying Apify actor can change
 * without affecting the rest of the system.
 *
 * Why exists: Holds outbound prospects for admin review before they are imported into
 * the main Lead table.
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@Entity
@Table(name = "discovered_leads", indexes = {
    @Index(name = "idx_discovered_leads_run_id", columnList = "lead_source_run_id"),
    @Index(name = "idx_discovered_leads_status", columnList = "status"),
    @Index(name = "idx_discovered_leads_email", columnList = "email"),
    @Index(name = "idx_discovered_leads_phone", columnList = "phone"),
    @Index(name = "idx_discovered_leads_website_url", columnList = "website_url")
})
public class DiscoveredLead extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_source_run_id", nullable = false)
    private LeadSourceRun leadSourceRun;

    @Column(name = "business_name", length = 500)
    private String businessName;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "contact_name", length = 255)
    private String contactName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "industry", length = 255)
    private String industry;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Lob
    @Column(name = "raw_data_json", columnDefinition = "TEXT")
    private String rawDataJson;

    @Column(name = "score")
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DiscoveredLeadStatus status = DiscoveredLeadStatus.NEW;

    @Column(name = "imported_lead_id")
    private UUID importedLeadId;

    public DiscoveredLead() {
        super();
    }

    public DiscoveredLead(UUID id) {
        super();
        this.id = id;
    }

    public LeadSourceRun getLeadSourceRun() {
        return leadSourceRun;
    }

    public void setLeadSourceRun(LeadSourceRun leadSourceRun) {
        this.leadSourceRun = leadSourceRun;
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
}
