package com.agentopscrm.agent;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Lead;
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
 * AI Agent for generating follow-up messages for qualified leads.
 * 
 * Uses OpenAI to generate safe, context-aware follow-up messages in three styles:
 * - PROFESSIONAL: Formal business tone
 * - FRIENDLY: Warm conversational tone
 * - SHORT_WHATSAPP: Brief WhatsApp-friendly message
 *
 * @author AgentOps Team
 * @version 0.7.0
 */
@Component
public class FollowUpAgent {

    private static final Logger log = LoggerFactory.getLogger(FollowUpAgent.class);
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String promptText;

    public FollowUpAgent(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${follow-up.model:gpt-4o-mini}") String model,
            @Value("classpath:prompts/follow-up-agent.md") Resource promptResource,
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
     * Generates follow-up messages for a lead using AI.
     *
     * @param lead Lead to generate messages for
     * @param business Associated business context
     * @return FollowUpMessages containing three message variations
     * @throws FollowUpGenerationException if AI generation fails
     */
    @SuppressWarnings("unchecked")
    public FollowUpMessages generateFollowUpMessages(Lead lead, Business business, String conversationSummary) 
            throws FollowUpGenerationException {
        if (!isConfigured()) {
            log.warn("OpenAI API not configured, using fallback messages");
            return generateFallbackMessages(lead, business);
        }

        log.info("Generating follow-up messages for lead ID: {}", lead.getId());

        try {
            // Build context from lead and business
            String context = buildContext(lead, business, conversationSummary);

            // Build the request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7); // Creative but consistent
            requestBody.put("max_tokens", 800);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", promptText));
            messages.add(Map.of("role", "user", "content", context));
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
                log.error("Invalid response from OpenAI API");
                return generateFallbackMessages(lead, business);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) {
                log.error("No choices in OpenAI response");
                return generateFallbackMessages(lead, business);
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Parse JSON response
            FollowUpMessages result = parseMessages(content);
            
            log.info("Successfully generated follow-up messages for lead ID: {}", lead.getId());
            return result;

        } catch (Exception e) {
            log.error("Failed to generate follow-up messages for lead ID: {}", lead.getId(), e);
            return generateFallbackMessages(lead, business);
        }
    }

    /**
     * Builds context string from lead and business information.
     */
    private String buildContext(Lead lead, Business business, String conversationSummary) {
        StringBuilder context = new StringBuilder();
        context.append("Lead Information:\n");
        context.append("Name: ").append(lead.getName() != null ? lead.getName() : "Unknown").append("\n");
        context.append("Requirement: ").append(lead.getRequirementText() != null ? lead.getRequirementText() : "Not specified").append("\n");
        
        if (lead.getBudget() != null) {
            context.append("Budget: $").append(lead.getBudget()).append("\n");
        }
        if (lead.getUrgency() != null) {
            context.append("Urgency: ").append(lead.getUrgency()).append("\n");
        }
        if (lead.getTimeline() != null) {
            context.append("Timeline: ").append(lead.getTimeline()).append("\n");
        }
        if (lead.getLeadScore() != null) {
            context.append("Lead Score: ").append(lead.getLeadScore()).append("/100\n");
        }

        context.append("\nBusiness Information:\n");
        context.append("Business Name: ").append(business.getName()).append("\n");
        if (business.getIndustry() != null && !business.getIndustry().isBlank()) {
            context.append("Industry: ").append(business.getIndustry()).append("\n");
        }
        if (business.getDescription() != null && !business.getDescription().isBlank()) {
            context.append("Description: ").append(business.getDescription()).append("\n");
        }

        if (conversationSummary != null && !conversationSummary.isBlank()) {
            context.append("\nConversation Summary:\n").append(conversationSummary).append("\n");
        }

        return context.toString();
    }

    /**
     * Parses JSON response from OpenAI into FollowUpMessages object.
     */
    private FollowUpMessages parseMessages(String jsonContent) throws FollowUpGenerationException {
        try {
            // Remove markdown code blocks if present
            String cleanJson = jsonContent.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceFirst("```json\\n?", "").replaceFirst("```$", "").trim();
            }

            @SuppressWarnings("unchecked")
            Map<String, String> parsed = objectMapper.readValue(cleanJson, Map.class);

            return new FollowUpMessages(
                    parsed.getOrDefault("professional", ""),
                    parsed.getOrDefault("friendly", ""),
                    parsed.getOrDefault("shortWhatsapp", "")
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to parse follow-up messages JSON: {}", jsonContent, e);
            throw new FollowUpGenerationException("Failed to parse AI response", e);
        }
    }

    /**
     * Generates simple rule-based fallback messages when AI is unavailable.
     */
    private FollowUpMessages generateFallbackMessages(Lead lead, Business business) {
        String leadName = lead.getName() != null ? lead.getName() : "there";
        String businessName = business.getName();
        String requirement = lead.getRequirementText() != null ? lead.getRequirementText() : "your requirement";

        String professional = String.format(
                "Dear %s, Thank you for your interest in %s. We have received your requirement regarding %s. " +
                "Our team is reviewing the details and will follow up with you soon to discuss how we can help. " +
                "Please let us know if you have any questions in the meantime.",
                leadName, businessName, requirement
        );

        String friendly = String.format(
                "Hi %s! Thanks for reaching out to %s about %s. " +
                "We'd love to help you with this. Our team is looking into it and we'll get back to you soon. " +
                "Feel free to reach out if you have any questions!",
                leadName, businessName, requirement
        );

        String shortWhatsapp = String.format(
                "Hi %s, thanks for your interest in %s. We'll review your requirement and get back to you soon!",
                leadName, businessName
        );

        return new FollowUpMessages(professional, friendly, shortWhatsapp);
    }

    /**
     * Container for three follow-up message variations.
     */
    public static class FollowUpMessages {
        private final String professional;
        private final String friendly;
        private final String shortWhatsapp;

        public FollowUpMessages(String professional, String friendly, String shortWhatsapp) {
            this.professional = professional;
            this.friendly = friendly;
            this.shortWhatsapp = shortWhatsapp;
        }

        public String getProfessional() {
            return professional;
        }

        public String getFriendly() {
            return friendly;
        }

        public String getShortWhatsapp() {
            return shortWhatsapp;
        }
    }

    /**
     * Exception thrown when follow-up message generation fails.
     */
    public static class FollowUpGenerationException extends Exception {
        public FollowUpGenerationException(String message) {
            super(message);
        }

        public FollowUpGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
