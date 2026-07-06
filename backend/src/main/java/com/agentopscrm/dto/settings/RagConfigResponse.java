package com.agentopscrm.dto.settings;

import java.time.Instant;

/**
 * RAG/Knowledge Base configuration and metrics response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class RagConfigResponse {
    private String embeddingProvider;
    private String embeddingModel;
    private Integer embeddingDimension;
    private String vectorStoreStrategy;
    private Integer defaultTopK;
    private Integer maxTopK;
    private Long totalBusinesses;
    private Long businessesWithDocuments;
    private Long businessesWithKnowledge;
    private Long totalDocuments;
    private Long totalKnowledgeChunks;
    private Instant lastSuccessfulBuild;
    private Instant lastFailedBuild;
    private String vectorStoreWarning;

    public RagConfigResponse() {
    }

    // Getters and setters
    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public String getVectorStoreStrategy() {
        return vectorStoreStrategy;
    }

    public void setVectorStoreStrategy(String vectorStoreStrategy) {
        this.vectorStoreStrategy = vectorStoreStrategy;
    }

    public Integer getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(Integer defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public Integer getMaxTopK() {
        return maxTopK;
    }

    public void setMaxTopK(Integer maxTopK) {
        this.maxTopK = maxTopK;
    }

    public Long getTotalBusinesses() {
        return totalBusinesses;
    }

    public void setTotalBusinesses(Long totalBusinesses) {
        this.totalBusinesses = totalBusinesses;
    }

    public Long getBusinessesWithDocuments() {
        return businessesWithDocuments;
    }

    public void setBusinessesWithDocuments(Long businessesWithDocuments) {
        this.businessesWithDocuments = businessesWithDocuments;
    }

    public Long getBusinessesWithKnowledge() {
        return businessesWithKnowledge;
    }

    public void setBusinessesWithKnowledge(Long businessesWithKnowledge) {
        this.businessesWithKnowledge = businessesWithKnowledge;
    }

    public Long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Long getTotalKnowledgeChunks() {
        return totalKnowledgeChunks;
    }

    public void setTotalKnowledgeChunks(Long totalKnowledgeChunks) {
        this.totalKnowledgeChunks = totalKnowledgeChunks;
    }

    public Instant getLastSuccessfulBuild() {
        return lastSuccessfulBuild;
    }

    public void setLastSuccessfulBuild(Instant lastSuccessfulBuild) {
        this.lastSuccessfulBuild = lastSuccessfulBuild;
    }

    public Instant getLastFailedBuild() {
        return lastFailedBuild;
    }

    public void setLastFailedBuild(Instant lastFailedBuild) {
        this.lastFailedBuild = lastFailedBuild;
    }

    public String getVectorStoreWarning() {
        return vectorStoreWarning;
    }

    public void setVectorStoreWarning(String vectorStoreWarning) {
        this.vectorStoreWarning = vectorStoreWarning;
    }
}
