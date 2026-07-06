package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.CrawlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Document entity representing a crawled webpage.
 *
 * Why exists: Stores raw crawled content from Firecrawl before processing
 * into knowledge chunks for RAG.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Entity
@Table(name = "documents")
public class Document extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // metadataJson removed due to JSONB type mapping issue
    // TODO: Add back with proper @Type(JsonType) annotation from hibernate-types

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CrawlStatus status = CrawlStatus.COMPLETED;

    public Document() {
        super();
    }

    public Document(UUID id) {
        super();
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public void setStatus(CrawlStatus status) {
        this.status = status;
    }
}