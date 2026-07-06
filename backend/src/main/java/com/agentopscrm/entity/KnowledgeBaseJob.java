package com.agentopscrm.entity;

import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks an asynchronous knowledge-base build job (Bug 2 fix).
 *
 * Why exists: The knowledge-base build (crawl/chunk/embed) can take longer than
 * frontend/proxy timeouts. Instead of blocking the HTTP request, the backend
 * accepts the build request immediately (202) and executes the work on a
 * background thread, persisting progress here so the frontend can poll for
 * real status/progress and recover an in-flight job after a page refresh.
 *
 * Extends {@link AuditableEntity} for createdAt/updatedAt bookkeeping;
 * {@code startedAt}/{@code completedAt} track the job lifecycle specifically.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
@Entity
@Table(name = "knowledge_base_jobs", indexes = {
    @Index(name = "idx_kb_jobs_business_id", columnList = "business_id"),
    @Index(name = "idx_kb_jobs_status", columnList = "status"),
    @Index(name = "idx_kb_jobs_started_at", columnList = "started_at")
})
public class KnowledgeBaseJob extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KnowledgeBaseJobStatus status = KnowledgeBaseJobStatus.QUEUED;

    @Column(name = "progress_percentage", nullable = false)
    private int progressPercentage = 0;

    @Column(name = "documents_total", nullable = false)
    private int documentsTotal = 0;

    @Column(name = "documents_processed", nullable = false)
    private int documentsProcessed = 0;

    @Column(name = "chunks_created", nullable = false)
    private int chunksCreated = 0;

    @Column(name = "embeddings_created", nullable = false)
    private int embeddingsCreated = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public KnowledgeBaseJob() {
        super();
    }

    public KnowledgeBaseJob(UUID id) {
        super();
        this.id = id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public KnowledgeBaseJobStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeBaseJobStatus status) {
        this.status = status;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public int getDocumentsTotal() {
        return documentsTotal;
    }

    public void setDocumentsTotal(int documentsTotal) {
        this.documentsTotal = documentsTotal;
    }

    public int getDocumentsProcessed() {
        return documentsProcessed;
    }

    public void setDocumentsProcessed(int documentsProcessed) {
        this.documentsProcessed = documentsProcessed;
    }

    public int getChunksCreated() {
        return chunksCreated;
    }

    public void setChunksCreated(int chunksCreated) {
        this.chunksCreated = chunksCreated;
    }

    public int getEmbeddingsCreated() {
        return embeddingsCreated;
    }

    public void setEmbeddingsCreated(int embeddingsCreated) {
        this.embeddingsCreated = embeddingsCreated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
