package com.agentopscrm.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for the Evaluation Agent (F-008 Evaluation Agent).
 *
 * Carries everything the Evaluation Agent needs to decide whether a Support
 * Chat Agent draft answer is grounded, factual and safe to send to a customer:
 * the customer question, the Support Agent's draft answer, and the exact
 * retrieved knowledge context (chunks + source URLs) that the answer must be
 * grounded in.
 *
 * @author AgentOps Team
 * @version 0.8.0
 */
public class EvaluationRequest {

    @NotNull(message = "Business ID is required")
    private UUID businessId;

    private UUID conversationId;

    private String question;

    private String draftAnswer;

    private List<String> retrievedChunks = new ArrayList<>();

    private List<String> sourceUrls = new ArrayList<>();

    public EvaluationRequest() {
    }

    public EvaluationRequest(UUID businessId,
                             UUID conversationId,
                             String question,
                             String draftAnswer,
                             List<String> retrievedChunks,
                             List<String> sourceUrls) {
        this.businessId = businessId;
        this.conversationId = conversationId;
        this.question = question;
        this.draftAnswer = draftAnswer;
        this.retrievedChunks = retrievedChunks != null ? retrievedChunks : new ArrayList<>();
        this.sourceUrls = sourceUrls != null ? sourceUrls : new ArrayList<>();
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDraftAnswer() {
        return draftAnswer;
    }

    public void setDraftAnswer(String draftAnswer) {
        this.draftAnswer = draftAnswer;
    }

    public List<String> getRetrievedChunks() {
        return retrievedChunks;
    }

    public void setRetrievedChunks(List<String> retrievedChunks) {
        this.retrievedChunks = retrievedChunks != null ? retrievedChunks : new ArrayList<>();
    }

    public List<String> getSourceUrls() {
        return sourceUrls;
    }

    public void setSourceUrls(List<String> sourceUrls) {
        this.sourceUrls = sourceUrls != null ? sourceUrls : new ArrayList<>();
    }
}
