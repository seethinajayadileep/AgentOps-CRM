package com.agentopscrm.dto;

import com.agentopscrm.entity.enums.KnowledgeBaseJobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for an asynchronous knowledge-base build job (Bug 2 fix).
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class KnowledgeBaseJobResponse {

    private UUID jobId;
    private UUID businessId;
    private KnowledgeBaseJobStatus status;
    private int progressPercentage;
    private int documentsTotal;
    private int documentsProcessed;
    private int chunksCreated;
    private int embeddingsCreated;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public KnowledgeBaseJobResponse() {
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
