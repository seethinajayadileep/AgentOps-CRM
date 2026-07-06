package com.agentopscrm.dto;

/**
 * Response DTO for the Evaluation Agent (F-008 Evaluation Agent).
 *
 * Represents the verdict of the Evaluation Agent for a single Support Chat
 * Agent draft answer. It reports how confident the evaluator is that the answer
 * is grounded ({@code confidenceScore}), the hallucination risk level, whether
 * the answer is safe to send to the customer, a short human-readable reason, and
 * (when unsafe) the safe fallback answer that should replace the draft.
 *
 * @author AgentOps Team
 * @version 0.8.0
 */
public class EvaluationResponse {

    /** 0-100. How confident we are that the draft is grounded in the chunks. */
    private int confidenceScore;

    /** LOW | MEDIUM | HIGH */
    private String hallucinationRisk;

    /** true = return the original draft; false = replace with safe fallback. */
    private boolean safeToSend;

    /** Short, clear reason. Never exposes internal prompt/system details. */
    private String reason;

    /**
     * When {@link #safeToSend} is false, the safe fallback answer to send to the
     * customer. When safe, this is {@code null} (caller keeps the original draft).
     */
    private String finalAnswer;

    public EvaluationResponse() {
    }

    public EvaluationResponse(int confidenceScore,
                              String hallucinationRisk,
                              boolean safeToSend,
                              String reason,
                              String finalAnswer) {
        this.confidenceScore = confidenceScore;
        this.hallucinationRisk = hallucinationRisk;
        this.safeToSend = safeToSend;
        this.reason = reason;
        this.finalAnswer = finalAnswer;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getHallucinationRisk() {
        return hallucinationRisk;
    }

    public void setHallucinationRisk(String hallucinationRisk) {
        this.hallucinationRisk = hallucinationRisk;
    }

    public boolean isSafeToSend() {
        return safeToSend;
    }

    public void setSafeToSend(boolean safeToSend) {
        this.safeToSend = safeToSend;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }
}
