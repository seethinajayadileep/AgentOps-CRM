package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.CrawlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business entity representing a customer business.
 *
 * Why exists: Core entity for the CRM - represents the businesses
 * that are using AgentOps to automate their customer interactions.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "businesses")
public class Business extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "website_url", nullable = false, length = 500)
    private String websiteUrl;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_status", nullable = false, length = 20)
    private CrawlStatus crawlStatus = CrawlStatus.NOT_STARTED;

    @Column(name = "crawl_started_at")
    private java.time.LocalDateTime crawlStartedAt;

    @Column(name = "crawl_finished_at")
    private java.time.LocalDateTime crawlFinishedAt;

    @Column(name = "crawl_error", columnDefinition = "TEXT")
    private String crawlError;

    @Column(name = "crawl_pages_saved", nullable = false)
    private int crawlPagesSaved = 0;

    @Column(name = "crawl_pages_total", nullable = false)
    private int crawlPagesTotal = 0;

    @OneToMany(mappedBy = "business")
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "business")
    private List<Conversation> conversations = new ArrayList<>();

    @OneToMany(mappedBy = "business")
    private List<Lead> leads = new ArrayList<>();

    public Business() {
        super();
    }

    public Business(UUID id) {
        super();
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

    public CrawlStatus getCrawlStatus() {
        return crawlStatus;
    }

    public void setCrawlStatus(CrawlStatus crawlStatus) {
        this.crawlStatus = crawlStatus;
    }

    public java.time.LocalDateTime getCrawlStartedAt() {
        return crawlStartedAt;
    }

    public void setCrawlStartedAt(java.time.LocalDateTime crawlStartedAt) {
        this.crawlStartedAt = crawlStartedAt;
    }

    public java.time.LocalDateTime getCrawlFinishedAt() {
        return crawlFinishedAt;
    }

    public void setCrawlFinishedAt(java.time.LocalDateTime crawlFinishedAt) {
        this.crawlFinishedAt = crawlFinishedAt;
    }

    public String getCrawlError() {
        return crawlError;
    }

    public void setCrawlError(String crawlError) {
        this.crawlError = crawlError;
    }

    public int getCrawlPagesSaved() {
        return crawlPagesSaved;
    }

    public void setCrawlPagesSaved(int crawlPagesSaved) {
        this.crawlPagesSaved = crawlPagesSaved;
    }

    public int getCrawlPagesTotal() {
        return crawlPagesTotal;
    }

    public void setCrawlPagesTotal(int crawlPagesTotal) {
        this.crawlPagesTotal = crawlPagesTotal;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }
}