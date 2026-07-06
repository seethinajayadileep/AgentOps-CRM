package com.agentopscrm.dto.settings;

/**
 * AI Models configuration response.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public class ModelsConfigResponse {
    private String ragAnswerModel;
    private String evaluationModel;
    private String leadQualificationModel;
    private String followUpModel;
    private String embeddingProvider;
    private String embeddingModel;
    private Integer embeddingDimension;
    private String configNote;

    public ModelsConfigResponse() {
    }

    // Getters and setters
    public String getRagAnswerModel() {
        return ragAnswerModel;
    }

    public void setRagAnswerModel(String ragAnswerModel) {
        this.ragAnswerModel = ragAnswerModel;
    }

    public String getEvaluationModel() {
        return evaluationModel;
    }

    public void setEvaluationModel(String evaluationModel) {
        this.evaluationModel = evaluationModel;
    }

    public String getLeadQualificationModel() {
        return leadQualificationModel;
    }

    public void setLeadQualificationModel(String leadQualificationModel) {
        this.leadQualificationModel = leadQualificationModel;
    }

    public String getFollowUpModel() {
        return followUpModel;
    }

    public void setFollowUpModel(String followUpModel) {
        this.followUpModel = followUpModel;
    }

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

    public String getConfigNote() {
        return configNote;
    }

    public void setConfigNote(String configNote) {
        this.configNote = configNote;
    }
}
