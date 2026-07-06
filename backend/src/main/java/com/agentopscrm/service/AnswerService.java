package com.agentopscrm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a grounded natural-language answer from retrieved knowledge chunks
 * using the configured AI provider (OpenAI Chat Completions).
 *
 * The prompt strictly instructs the model to answer ONLY from the supplied
 * context and to emit a fixed fallback sentence when the context is insufficient,
 * so the RAG flow does not hallucinate. See {@link RagService} for orchestration.
 *
 * @author AgentOps Team
 * @version 0.4.0
 */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    /** Returned verbatim by the model (and by RagService) when context is insufficient. */
    public static final String INSUFFICIENT_CONTEXT_ANSWER =
            "I could not find enough knowledge base content to answer this confidently.";

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions about ONE specific business
            using ONLY the provided context excerpts taken from that business's own website.

            Rules:
            - Use ONLY information present in the context. Do NOT use outside knowledge.
            - Ignore navigation menus, link lists, headers/footers and boilerplate.
            - If the context does not contain enough information to answer the question,
              reply with EXACTLY this sentence and nothing else:
              "%s"
            - Be concise and clear (2-6 sentences). Do not output markdown links or menus.
            """.formatted(INSUFFICIENT_CONTEXT_ANSWER);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public AnswerService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${rag.answer.model:gpt-4o-mini}") String model,
            RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Generate a grounded answer.
     *
     * @param query          the user's question
     * @param contextExcerpts already-cleaned context strings (each optionally prefixed with its source)
     * @return the model's answer text
     * @throws AnswerException if the provider call fails
     */
    @SuppressWarnings("unchecked")
    public String generateAnswer(String query, List<String> contextExcerpts) throws AnswerException {
        if (!isConfigured()) {
            throw new AnswerException("OPENAI_API_KEY is not configured");
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < contextExcerpts.size(); i++) {
            context.append("[").append(i + 1).append("] ").append(contextExcerpts.get(i)).append("\n\n");
        }

        String userContent = "Question: " + query + "\n\nContext:\n" + context;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0);
        body.put("max_tokens", 400);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    OPENAI_CHAT_URL, new HttpEntity<>(body, headers), Map.class);

            if (response == null) {
                throw new AnswerException("No response from AI provider");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AnswerException("AI provider returned no choices");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = message != null ? (String) message.get("content") : null;
            if (content == null || content.isBlank()) {
                throw new AnswerException("AI provider returned an empty answer");
            }
            return content.trim();
        } catch (AnswerException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI answer generation failed", e);
            throw new AnswerException("AI answer generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Custom exception for answer-generation errors.
     */
    public static class AnswerException extends Exception {
        public AnswerException(String message) {
            super(message);
        }

        public AnswerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
