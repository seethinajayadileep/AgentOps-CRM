package com.agentopscrm.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * AI Agent for lead qualification and extraction from customer messages.
 * 
 * Uses OpenAI to analyze chat messages and extract lead information including:
 * name, email, phone, requirements, budget, urgency, timeline, and summary.
 *
 * @author AgentOps Team
 * @version 0.6.0
 */
@Component
public class LeadQualificationAgent {

    private static final Logger log = LoggerFactory.getLogger(LeadQualificationAgent.class);
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String promptText;

    public LeadQualificationAgent(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${lead.qualification.model:gpt-4o-mini}") String model,
            @Value("classpath:prompts/lead-qualification-agent.md") Resource promptResource,
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
     * Extracts lead information from a customer message using AI.
     *
     * @param message Customer message to analyze
     * @return LeadExtractionResult containing extracted information
     * @throws LeadQualificationException if AI extraction fails
     */
    @SuppressWarnings("unchecked")
    public LeadExtractionResult extractLeadInfo(String message) throws LeadQualificationException {
        if (!isConfigured()) {
            throw new LeadQualificationException("OPENAI_API_KEY is not configured");
        }

        log.info("Extracting lead info from message: {}", message.substring(0, Math.min(100, message.length())));

        try {
            // Build the request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0); // Deterministic extraction
            requestBody.put("max_tokens", 500);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", promptText));
            messages.add(Map.of("role", "user", "content", message));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Call OpenAI
            Map<String, Object> response = restTemplate.postForObject(
                    OPENAI_CHAT_URL,
                    entity,
                    Map.class
            );

            if (response == null || !response.containsKey("choices")) {
                throw new LeadQualificationException("Invalid OpenAI API response");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LeadQualificationException("No choices in OpenAI response");
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
            String content = (String) messageObj.get("content");

            log.info("AI extracted content: {}", content);

            // Parse JSON response
            LeadExtractionResult result = parseExtractionResult(content);
            log.info("Successfully extracted lead info: name={}, email={}, phone={}", 
                    result.getName(), result.getEmail(), result.getPhone());

            return result;

        } catch (Exception e) {
            log.error("Lead extraction failed", e);
            throw new LeadQualificationException("Failed to extract lead information: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the JSON response from AI into LeadExtractionResult.
     */
    private LeadExtractionResult parseExtractionResult(String jsonContent) throws LeadQualificationException {
        try {
            // Clean the content if it has markdown code blocks
            String cleanedContent = jsonContent.trim();
            if (cleanedContent.startsWith("```json")) {
                cleanedContent = cleanedContent.substring(7);
            }
            if (cleanedContent.startsWith("```")) {
                cleanedContent = cleanedContent.substring(3);
            }
            if (cleanedContent.endsWith("```")) {
                cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3);
            }
            cleanedContent = cleanedContent.trim();

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(cleanedContent, Map.class);

            LeadExtractionResult result = new LeadExtractionResult();
            result.setName(getStringOrNull(map.get("name")));
            result.setEmail(getStringOrNull(map.get("email")));
            result.setPhone(getStringOrNull(map.get("phone")));
            result.setRequirementText(getStringOrNull(map.get("requirementText")));
            result.setBudget(getStringOrNull(map.get("budget")));
            result.setUrgency(getStringOrNull(map.get("urgency")));
            result.setTimeline(getStringOrNull(map.get("timeline")));
            result.setSummary(getStringOrNull(map.get("summary")));

            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI JSON response: {}", jsonContent, e);
            throw new LeadQualificationException("Failed to parse AI response as JSON", e);
        }
    }

    /**
     * Safely extracts string value from object, handling nulls.
     */
    private String getStringOrNull(Object value) {
        if (value == null || "null".equals(value)) {
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    /**
     * Detects if a message contains buying intent keywords.
     */
    public boolean detectBuyingIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        
        // Buying intent keywords
        String[] keywords = {
            "price", "pricing", "cost", "quote",
            "buy", "purchase", "order",
            "interested", "need", "want",
            "call me", "contact me", "demo",
            "start", "begin", "launch",
            "budget", "payment", "plan", "package",
            "how much", "what does it cost", "what is the cost",
            "urgent", "asap", "soon"
        };

        for (String keyword : keywords) {
            if (lowerMessage.contains(keyword)) {
                log.info("Detected buying intent keyword: {}", keyword);
                return true;
            }
        }

        return false;
    }

    /**
     * Result object for lead extraction.
     */
    public static class LeadExtractionResult {
        private String name;
        private String email;
        private String phone;
        private String requirementText;
        private String budget;
        private String urgency;
        private String timeline;
        private String summary;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getRequirementText() {
            return requirementText;
        }

        public void setRequirementText(String requirementText) {
            this.requirementText = requirementText;
        }

        public String getBudget() {
            return budget;
        }

        public void setBudget(String budget) {
            this.budget = budget;
        }

        public String getUrgency() {
            return urgency;
        }

        public void setUrgency(String urgency) {
            this.urgency = urgency;
        }

        public String getTimeline() {
            return timeline;
        }

        public void setTimeline(String timeline) {
            this.timeline = timeline;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }

    /**
     * Exception thrown when lead qualification fails.
     */
    public static class LeadQualificationException extends Exception {
        public LeadQualificationException(String message) {
            super(message);
        }

        public LeadQualificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
