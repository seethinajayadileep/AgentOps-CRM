package com.agentopscrm.service;

import com.agentopscrm.agent.EvaluationAgent;
import com.agentopscrm.dto.EvaluationRequest;
import com.agentopscrm.dto.EvaluationResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestration service for the Evaluation Agent (F-008).
 *
 * Responsibilities:
 *  - Run the Evaluation Agent (LLM path first, deterministic rule-based fallback
 *    when the LLM is unavailable or fails).
 *  - Persist a complete AgentLog audit trail for every evaluation:
 *    EVALUATION_STARTED, EVALUATION_COMPLETED, EVALUATION_FAILED,
 *    UNSAFE_ANSWER_BLOCKED, FALLBACK_ANSWER_USED (customer-facing fallback),
 *    and FALLBACK_USED (rule-based engine used because LLM failed).
 *  - Never throw to the caller for a safety verdict: if everything fails we
 *    fail closed by returning an unsafe verdict with the safe fallback answer.
 *
 * Keeping all evaluation orchestration here keeps {@link ChatService} thin and
 * lets the optional {@code POST /api/evaluation/test} endpoint reuse the exact
 * same logic.
 *
 * @author AgentOps Team
 * @version 0.8.0
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private static final String AGENT_NAME = "EvaluationAgent";

    private final EvaluationAgent evaluationAgent;
    private final BusinessRepository businessRepository;
    private final ConversationRepository conversationRepository;
    private final AgentLogRepository agentLogRepository;
    private final ObjectMapper objectMapper;

    public EvaluationService(EvaluationAgent evaluationAgent,
                             BusinessRepository businessRepository,
                             ConversationRepository conversationRepository,
                             AgentLogRepository agentLogRepository,
                             ObjectMapper objectMapper) {
        this.evaluationAgent = evaluationAgent;
        this.businessRepository = businessRepository;
        this.conversationRepository = conversationRepository;
        this.agentLogRepository = agentLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate a Support Agent draft answer and return the safety verdict.
     *
     * This method never throws for a normal evaluation problem: on any failure it
     * fails closed with an unsafe verdict + safe fallback answer, and records an
     * EVALUATION_FAILED AgentLog.
     */
    public EvaluationResponse evaluate(EvaluationRequest request) {
        Business business = resolveBusiness(request.getBusinessId());
        Conversation conversation = resolveConversation(request.getConversationId());

        String inputJson = toJson(request);

        // 1. evaluation started
        saveLog(business, conversation, "EVALUATION_STARTED", inputJson,
                "{\"status\":\"started\"}", AgentActionStatus.SUCCESS, null);

        EvaluationResponse result;
        boolean usedRuleFallback = false;

        // 2. LLM path first, deterministic rules on failure.
        try {
            if (evaluationAgent.isConfigured()) {
                result = evaluationAgent.evaluateWithLlm(request);
            } else {
                usedRuleFallback = true;
                result = evaluationAgent.evaluateWithRules(request);
            }
        } catch (EvaluationAgent.EvaluationException e) {
            log.warn("Evaluation LLM failed, using rule-based fallback: {}", e.getMessage());
            usedRuleFallback = true;
            try {
                result = evaluationAgent.evaluateWithRules(request);
                // rule-based engine used because LLM failed
                saveLog(business, conversation, "FALLBACK_USED", inputJson,
                        toJson(result), AgentActionStatus.FALLBACK_USED, e.getMessage());
            } catch (Exception ruleError) {
                // 3. evaluation failed (everything failed) -> fail closed.
                log.error("Rule-based evaluation also failed, failing closed", ruleError);
                result = failClosed();
                saveLog(business, conversation, "EVALUATION_FAILED", inputJson,
                        toJson(result), AgentActionStatus.FAILED, ruleError.getMessage());
                saveLog(business, conversation, "FALLBACK_ANSWER_USED", inputJson,
                        toJson(result), AgentActionStatus.FALLBACK_USED,
                        "Safe fallback returned after total evaluation failure.");
                return result;
            }
        } catch (Exception e) {
            log.error("Unexpected evaluation error, failing closed", e);
            result = failClosed();
            saveLog(business, conversation, "EVALUATION_FAILED", inputJson,
                    toJson(result), AgentActionStatus.FAILED, e.getMessage());
            saveLog(business, conversation, "FALLBACK_ANSWER_USED", inputJson,
                    toJson(result), AgentActionStatus.FALLBACK_USED,
                    "Safe fallback returned after unexpected evaluation failure.");
            return result;
        }

        // 4. unsafe answer blocked + fallback answer used
        if (!result.isSafeToSend()) {
            if (result.getFinalAnswer() == null || result.getFinalAnswer().isBlank()) {
                result.setFinalAnswer(EvaluationAgent.SAFE_FALLBACK_ANSWER);
            }
            saveLog(business, conversation, "UNSAFE_ANSWER_BLOCKED", inputJson,
                    toJson(result), AgentActionStatus.SUCCESS, result.getReason());
            saveLog(business, conversation, "FALLBACK_ANSWER_USED", inputJson,
                    toJson(result), AgentActionStatus.FALLBACK_USED,
                    "Draft answer replaced with safe fallback.");
        }

        // 5. evaluation completed
        AgentActionStatus completedStatus = usedRuleFallback
                ? AgentActionStatus.FALLBACK_USED
                : AgentActionStatus.SUCCESS;
        saveLog(business, conversation, "EVALUATION_COMPLETED", inputJson,
                toJson(result), completedStatus, null);

        return result;
    }

    /**
     * Fail-closed verdict: unsafe + safe fallback answer.
     */
    private EvaluationResponse failClosed() {
        return new EvaluationResponse(
                0,
                EvaluationAgent.RISK_HIGH,
                false,
                "Evaluation could not be completed; sending safe fallback.",
                EvaluationAgent.SAFE_FALLBACK_ANSWER);
    }

    private Business resolveBusiness(UUID businessId) {
        if (businessId == null) {
            return null;
        }
        return businessRepository.findById(businessId).orElse(null);
    }

    private Conversation resolveConversation(UUID conversationId) {
        if (conversationId == null) {
            return null;
        }
        return conversationRepository.findById(conversationId).orElse(null);
    }

    private void saveLog(Business business,
                         Conversation conversation,
                         String action,
                         String inputJson,
                         String outputJson,
                         AgentActionStatus status,
                         String errorMessage) {
        try {
            AgentLog logEntry = new AgentLog();
            logEntry.setBusiness(business);
            logEntry.setConversation(conversation);
            logEntry.setAgentName(AGENT_NAME);
            logEntry.setAction(action);
            logEntry.setInputJson(inputJson);
            logEntry.setOutputJson(outputJson);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(errorMessage);
            agentLogRepository.save(logEntry);
        } catch (Exception e) {
            // Logging must never break the chat/evaluation flow.
            log.error("Failed to persist AgentLog for action {}", action, e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
