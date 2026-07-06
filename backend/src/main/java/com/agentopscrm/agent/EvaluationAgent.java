package com.agentopscrm.agent;

import com.agentopscrm.dto.EvaluationRequest;
import com.agentopscrm.dto.EvaluationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluation Agent (F-008).
 *
 * Acts as a safety gate between the Support Chat Agent's draft answer and the
 * customer. It verifies that a draft answer is grounded in the retrieved
 * business knowledge and does not invent pricing, discounts, delivery
 * guarantees, unsupported services, or legal/refund/payment claims.
 *
 * Two evaluation paths:
 *  1. {@link #evaluateWithLlm(EvaluationRequest)} - primary, uses OpenAI with the
 *     {@code prompts/evaluation-agent.md} system prompt and returns strict JSON.
 *  2. {@link #evaluateWithRules(EvaluationRequest)} - deterministic fallback used
 *     when the LLM is not configured or the call fails. Keyword-based risky-claim
 *     detection cross-checked against the retrieved chunks.
 *
 * @author AgentOps Team
 * @version 0.8.0
 */
@Component
public class EvaluationAgent {

    private static final Logger log = LoggerFactory.getLogger(EvaluationAgent.class);
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    /** Customer-facing safe fallback used whenever an answer is unsafe. */
    public static final String SAFE_FALLBACK_ANSWER =
            "I do not have confirmed information about that. Please share your contact details and our team will help you.";

    // Rule-based risky keyword groups (F-008 spec).
    private static final String[] PRICING_KEYWORDS = {
            "price", "pricing", "cost", "₹", "rupees", "inr", "dollar", "usd", "starts from", "package"
    };
    private static final String[] DISCOUNT_KEYWORDS = {
            "discount", "offer", "free", "coupon", "deal"
    };
    private static final String[] GUARANTEE_KEYWORDS = {
            "guarantee", "guaranteed", "sure", "definitely", "delivery", "delivered in", "completed in"
    };
    private static final String[] LEGAL_KEYWORDS = {
            "refund", "legal", "contract", "payment confirmed", "invoice", "cancellation"
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String promptText;

    public EvaluationAgent(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${evaluation.agent.model:gpt-4o-mini}") String model,
            @Value("classpath:prompts/evaluation-agent.md") Resource promptResource,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) throws IOException {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptText = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Primary LLM-based evaluation. Sends the question, retrieved chunks, source
     * URLs and draft answer to the model and parses the strict JSON verdict.
     *
     * @throws EvaluationException if the LLM is not configured or the call fails
     */
    @SuppressWarnings("unchecked")
    public EvaluationResponse evaluateWithLlm(EvaluationRequest request) throws EvaluationException {
        if (!isConfigured()) {
            throw new EvaluationException("OPENAI_API_KEY is not configured");
        }

        String userContent = buildUserContent(request);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", promptText));
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0);
        body.put("max_tokens", 400);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    OPENAI_CHAT_URL, new HttpEntity<>(body, headers), Map.class);

            if (response == null || !response.containsKey("choices")) {
                throw new EvaluationException("Invalid OpenAI API response");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new EvaluationException("No choices in OpenAI response");
            }
            Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
            String content = messageObj != null ? (String) messageObj.get("content") : null;
            if (content == null || content.isBlank()) {
                throw new EvaluationException("Empty evaluation content");
            }

            EvaluationResponse result = parseJson(content);

            // Safety net: enforce the empty-chunks rule regardless of the model.
            if (request.getRetrievedChunks() == null || request.getRetrievedChunks().isEmpty()) {
                result.setSafeToSend(false);
                result.setHallucinationRisk(RISK_HIGH);
                if (result.getReason() == null || result.getReason().isBlank()) {
                    result.setReason("No retrieved knowledge to support the answer.");
                }
            }

            // Consistency: an unsafe answer must never be labelled LOW risk.
            if (!result.isSafeToSend() && RISK_LOW.equals(result.getHallucinationRisk())) {
                result.setHallucinationRisk(RISK_HIGH);
            }

            // Ensure fallback answer is present when unsafe; cleared when safe.
            if (!result.isSafeToSend()) {
                result.setFinalAnswer(SAFE_FALLBACK_ANSWER);
            } else {
                result.setFinalAnswer(null);
            }
            return result;

        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Evaluation LLM call failed", e);
            throw new EvaluationException("Evaluation LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deterministic, rule-based evaluation used as a fallback when the LLM path
     * is unavailable or fails. Flags unsupported pricing, discount, guarantee and
     * legal/refund claims by cross-checking risky keywords against the chunks.
     */
    public EvaluationResponse evaluateWithRules(EvaluationRequest request) {
        List<String> chunks = request.getRetrievedChunks();
        String draft = request.getDraftAnswer() == null ? "" : request.getDraftAnswer();
        String draftLower = draft.toLowerCase();
        String chunksLower = joinLower(chunks);

        // Rule 5 / empty chunks -> always unsafe.
        if (chunks == null || chunks.isEmpty()) {
            return new EvaluationResponse(0, RISK_HIGH, false,
                    "No retrieved knowledge to support the answer.", SAFE_FALLBACK_ANSWER);
        }

        // Pricing invented?
        if (containsAny(draftLower, PRICING_KEYWORDS) && !containsAny(chunksLower, PRICING_KEYWORDS)) {
            return new EvaluationResponse(30, RISK_HIGH, false,
                    "Answer mentioned pricing but pricing was not found in retrieved chunks.",
                    SAFE_FALLBACK_ANSWER);
        }

        // Discount promised?
        if (containsAny(draftLower, DISCOUNT_KEYWORDS) && !containsAny(chunksLower, DISCOUNT_KEYWORDS)) {
            return new EvaluationResponse(30, RISK_HIGH, false,
                    "Answer promised a discount that was not found in retrieved chunks.",
                    SAFE_FALLBACK_ANSWER);
        }

        // Guarantee / delivery timeline?
        if (containsAny(draftLower, GUARANTEE_KEYWORDS) && !containsAny(chunksLower, GUARANTEE_KEYWORDS)) {
            return new EvaluationResponse(35, RISK_HIGH, false,
                    "Answer guaranteed delivery/results not supported by retrieved chunks.",
                    SAFE_FALLBACK_ANSWER);
        }

        // Legal / refund / payment / contract claims?
        if (containsAny(draftLower, LEGAL_KEYWORDS) && !containsAny(chunksLower, LEGAL_KEYWORDS)) {
            return new EvaluationResponse(35, RISK_HIGH, false,
                    "Answer made legal/refund/payment claims not supported by retrieved chunks.",
                    SAFE_FALLBACK_ANSWER);
        }

        // No risky-claim mismatches detected -> treat as safe (LOW risk).
        return new EvaluationResponse(80, RISK_LOW, true,
                "Answer is supported by retrieved chunks.", null);
    }

    /**
     * Builds the user message with the customer question, chunks, source URLs and
     * the draft answer for LLM evaluation.
     */
    private String buildUserContent(EvaluationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer question:\n").append(nullSafe(request.getQuestion())).append("\n\n");

        sb.append("Retrieved knowledge chunks:\n");
        List<String> chunks = request.getRetrievedChunks();
        if (chunks == null || chunks.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (int i = 0; i < chunks.size(); i++) {
                sb.append("[").append(i + 1).append("] ").append(chunks.get(i)).append("\n");
            }
        }
        sb.append("\n");

        sb.append("Source URLs:\n");
        List<String> urls = request.getSourceUrls();
        if (urls == null || urls.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (String u : urls) {
                sb.append("- ").append(u).append("\n");
            }
        }
        sb.append("\n");

        sb.append("Draft answer generated by Support Agent:\n")
                .append(nullSafe(request.getDraftAnswer()));
        return sb.toString();
    }

    /**
     * Parses the model's JSON verdict, tolerating markdown code fences.
     */
    private EvaluationResponse parseJson(String content) throws EvaluationException {
        try {
            String cleaned = content.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(cleaned, Map.class);

            EvaluationResponse r = new EvaluationResponse();
            r.setConfidenceScore(toInt(map.get("confidenceScore")));
            r.setHallucinationRisk(normalizeRisk(map.get("hallucinationRisk")));
            r.setSafeToSend(toBool(map.get("safeToSend")));
            r.setReason(toStringOrNull(map.get("reason")));
            r.setFinalAnswer(toStringOrNull(map.get("finalAnswer")));
            return r;
        } catch (Exception e) {
            log.error("Failed to parse evaluation JSON: {}", content, e);
            throw new EvaluationException("Failed to parse evaluation JSON", e);
        }
    }

    private String normalizeRisk(Object value) {
        if (value == null) {
            return RISK_MEDIUM;
        }
        String s = value.toString().trim().toUpperCase();
        if (RISK_LOW.equals(s) || RISK_MEDIUM.equals(s) || RISK_HIGH.equals(s)) {
            return s;
        }
        return RISK_MEDIUM;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean toBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && "true".equalsIgnoreCase(value.toString().trim());
    }

    private String toStringOrNull(Object value) {
        if (value == null || "null".equals(value)) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String joinLower(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        return String.join(" \n ", chunks).toLowerCase();
    }

    private boolean containsAny(String haystack, String[] keywords) {
        if (haystack == null || haystack.isEmpty()) {
            return false;
        }
        for (String kw : keywords) {
            if (haystack.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Exception thrown when the LLM evaluation path fails.
     */
    public static class EvaluationException extends Exception {
        public EvaluationException(String message) {
            super(message);
        }

        public EvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
