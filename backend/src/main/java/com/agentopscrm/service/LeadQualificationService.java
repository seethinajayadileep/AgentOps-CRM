package com.agentopscrm.service;

import com.agentopscrm.agent.LeadQualificationAgent;
import com.agentopscrm.dto.LeadQualificationRequest;
import com.agentopscrm.dto.LeadQualificationResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.LeadStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for qualifying leads from customer messages.
 * 
 * Responsibilities:
 * - Detect buying intent
 * - Extract lead information using AI
 * - Calculate lead score
 * - Create or update leads
 * - Prevent duplicates
 * - Log all actions
 *
 * @author AgentOps Team
 * @version 0.6.0
 */
@Service
public class LeadQualificationService {

    private static final Logger log = LoggerFactory.getLogger(LeadQualificationService.class);

    private final LeadQualificationAgent agent;
    private final LeadRepository leadRepository;
    private final BusinessRepository businessRepository;
    private final ConversationRepository conversationRepository;
    private final AgentLogRepository agentLogRepository;
    private final ObjectMapper objectMapper;

    public LeadQualificationService(
            LeadQualificationAgent agent,
            LeadRepository leadRepository,
            BusinessRepository businessRepository,
            ConversationRepository conversationRepository,
            AgentLogRepository agentLogRepository,
            ObjectMapper objectMapper) {
        this.agent = agent;
        this.leadRepository = leadRepository;
        this.businessRepository = businessRepository;
        this.conversationRepository = conversationRepository;
        this.agentLogRepository = agentLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Qualifies a lead from a customer message.
     * 
     * @param request Lead qualification request
     * @return Lead qualification response
     */
    @Transactional
    public LeadQualificationResponse qualifyLead(LeadQualificationRequest request) {
        long startTime = System.currentTimeMillis();
        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new IllegalArgumentException("Business not found"));

        Conversation conversation = null;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElse(null);
        }

        log.info("Qualifying lead - businessId: {}, conversationId: {}", 
                request.getBusinessId(), request.getConversationId());

        // Log qualification start
        logAction(business, conversation, null, "lead_qualification_started", 
                createInputJson(request), null, AgentActionStatus.SUCCESS, null, 0L);

        try {
            // Check for existing lead by conversationId (duplicate prevention)
            Lead existingLead = null;
            if (conversation != null) {
                Optional<Lead> leadOpt = leadRepository.findByConversationId(conversation.getId());
                existingLead = leadOpt.orElse(null);
            }

            // Extract lead info using AI
            LeadQualificationAgent.LeadExtractionResult extraction;
            try {
                extraction = agent.extractLeadInfo(request.getMessage());
            } catch (LeadQualificationAgent.LeadQualificationException e) {
                log.warn("AI extraction failed, using fallback", e);
                extraction = fallbackExtraction(request.getMessage());
                logAction(business, conversation, null, "fallback_extraction_used",
                        createInputJson(request), toJson(extraction), AgentActionStatus.FALLBACK_USED, 
                        e.getMessage(), System.currentTimeMillis() - startTime);
            }

            // Create or update lead
            Lead lead;
            if (existingLead != null) {
                lead = updateExistingLead(existingLead, extraction);
                log.info("Updated existing lead: {}", lead.getId());
                logAction(business, conversation, lead, "lead_updated",
                        createInputJson(request), toJson(extraction), AgentActionStatus.SUCCESS, 
                        null, System.currentTimeMillis() - startTime);
            } else {
                lead = createNewLead(business, conversation, extraction);
                log.info("Created new lead: {}", lead.getId());
                logAction(business, conversation, lead, "lead_created",
                        createInputJson(request), toJson(extraction), AgentActionStatus.SUCCESS, 
                        null, System.currentTimeMillis() - startTime);
            }

            // Calculate lead score
            double score = calculateLeadScore(lead);
            lead.setLeadScore(score);

            // Determine status based on score
            LeadStatus status = determineStatus(score);
            lead.setStatus(status);

            // Save lead
            lead = leadRepository.save(lead);

            // Update conversation with lead contact information
            if (conversation != null) {
                updateConversationContact(conversation, lead);
            }

            // Build response
            LeadQualificationResponse response = buildResponse(lead, extraction);

            logAction(business, conversation, lead, "lead_qualification_completed",
                    createInputJson(request), toJson(response), AgentActionStatus.SUCCESS, 
                    null, System.currentTimeMillis() - startTime);

            return response;

        } catch (Exception e) {
            log.error("Lead qualification failed", e);
            logAction(business, conversation, null, "lead_qualification_failed",
                    createInputJson(request), null, AgentActionStatus.FAILED, 
                    e.getMessage(), System.currentTimeMillis() - startTime);
            throw new RuntimeException("Failed to qualify lead: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new lead entity from extraction result.
     */
    private Lead createNewLead(Business business, Conversation conversation, 
                               LeadQualificationAgent.LeadExtractionResult extraction) {
        // Validate minimum required fields
        String name = extraction.getName();
        String email = extraction.getEmail();
        String phone = extraction.getPhone();

        // Reject if name is Unknown or blank
        if (name == null || name.isBlank() || name.equals("Unknown")) {
            log.warn("Rejecting lead creation: name is '{}' (must not be Unknown or blank)", name);
            throw new IllegalArgumentException("Cannot create lead with name 'Unknown' or blank. Name is required.");
        }

        // Reject if no email AND no phone
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            log.warn("Rejecting lead creation: no contact information (email: {}, phone: {})", email, phone);
            throw new IllegalArgumentException("Cannot create lead without contact information. Email or phone is required.");
        }

        // If conversation has pending lead data, use it to fill in missing fields
        if (conversation != null) {
            if (name == null || name.isBlank()) {
                name = conversation.getPendingLeadName();
            }
            if (email == null || email.isBlank()) {
                email = conversation.getPendingLeadEmail();
            }
            if (phone == null || phone.isBlank()) {
                phone = conversation.getPendingLeadPhone();
            }
        }

        Lead lead = new Lead();
        lead.setBusiness(business);
        lead.setConversation(conversation);
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setRequirementText(extraction.getRequirementText());
        lead.setBudget(parseBudget(extraction.getBudget()));
        lead.setUrgency(extraction.getUrgency());
        lead.setTimeline(extraction.getTimeline());
        lead.setSummary(extraction.getSummary());
        lead.setStatus(LeadStatus.NEW);
        return lead;
    }

    /**
     * Updates existing lead with new extraction, preserving non-null values.
     */
    private Lead updateExistingLead(Lead existing, LeadQualificationAgent.LeadExtractionResult extraction) {
        // Update only if new value is not null
        if (extraction.getName() != null && !extraction.getName().equals("Unknown")) {
            existing.setName(extraction.getName());
        }
        if (extraction.getEmail() != null) {
            existing.setEmail(extraction.getEmail());
        }
        if (extraction.getPhone() != null) {
            existing.setPhone(extraction.getPhone());
        }
        if (extraction.getRequirementText() != null) {
            existing.setRequirementText(extraction.getRequirementText());
        }
        if (extraction.getBudget() != null) {
            BigDecimal budget = parseBudget(extraction.getBudget());
            if (budget != null) {
                existing.setBudget(budget);
            }
        }
        if (extraction.getUrgency() != null) {
            existing.setUrgency(extraction.getUrgency());
        }
        if (extraction.getTimeline() != null) {
            existing.setTimeline(extraction.getTimeline());
        }
        if (extraction.getSummary() != null) {
            existing.setSummary(extraction.getSummary());
        }
        return existing;
    }

    /**
     * Calculates lead score based on available information.
     * 
     * Scoring rules:
     * - +20 if phone exists
     * - +20 if email exists
     * - +20 if clear requirement exists
     * - +15 if budget exists
     * - +15 if urgency or timeline exists
     * - +10 if calling/demo mentioned
     */
    private double calculateLeadScore(Lead lead) {
        double score = 0;

        if (lead.getPhone() != null && !lead.getPhone().isBlank()) {
            score += 20;
        }
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) {
            score += 20;
        }
        if (lead.getRequirementText() != null && !lead.getRequirementText().isBlank()) {
            score += 20;
        }
        if (lead.getBudget() != null) {
            score += 15;
        }
        if (lead.getUrgency() != null && !lead.getUrgency().isBlank()) {
            score += 15;
        } else if (lead.getTimeline() != null && !lead.getTimeline().isBlank()) {
            score += 15;
        }

        // Check for call/demo request in urgency or summary
        String urgency = lead.getUrgency() != null ? lead.getUrgency().toLowerCase() : "";
        String summary = lead.getSummary() != null ? lead.getSummary().toLowerCase() : "";
        if (urgency.contains("call") || urgency.contains("demo") || 
            summary.contains("call") || summary.contains("demo")) {
            score += 10;
        }

        return Math.min(score, 100); // Cap at 100
    }

    /**
     * Determines lead status based on score.
     * 
     * - score >= 75 = HOT
     * - score >= 50 = QUALIFIED
     * - score < 50 = NEW
     */
    private LeadStatus determineStatus(double score) {
        if (score >= 75) {
            return LeadStatus.HOT;
        } else if (score >= 50) {
            return LeadStatus.QUALIFIED;
        } else {
            return LeadStatus.NEW;
        }
    }

    /**
     * Fallback extraction using regex when AI fails.
     */
    private LeadQualificationAgent.LeadExtractionResult fallbackExtraction(String message) {
        LeadQualificationAgent.LeadExtractionResult result = new LeadQualificationAgent.LeadExtractionResult();

        // Extract email using regex
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher emailMatcher = emailPattern.matcher(message);
        if (emailMatcher.find()) {
            result.setEmail(emailMatcher.group());
        }

        // Extract phone using regex (Indian format)
        Pattern phonePattern = Pattern.compile("\\b[6-9]\\d{9}\\b|\\+91[\\s-]?[6-9]\\d{9}");
        Matcher phoneMatcher = phonePattern.matcher(message);
        if (phoneMatcher.find()) {
            result.setPhone(phoneMatcher.group());
        }

        // Detect budget keywords
        if (message.toLowerCase().matches(".*(rupees?|inr|₹|dollars?|usd|\\$).*")) {
            result.setBudget("Mentioned in message");
        }

        // Detect timeline keywords
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("today") || lowerMessage.contains("tomorrow") || 
            lowerMessage.contains("this week") || lowerMessage.contains("urgent") || 
            lowerMessage.contains("asap")) {
            result.setTimeline("Urgent");
        }

        result.setSummary("Lead detected with fallback extraction");
        return result;
    }

    /**
     * Parses budget string to BigDecimal (extracts numbers).
     */
    private BigDecimal parseBudget(String budgetStr) {
        if (budgetStr == null || budgetStr.isBlank()) {
            return null;
        }

        try {
            // Extract first number from string
            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
            Matcher matcher = pattern.matcher(budgetStr);
            if (matcher.find()) {
                return new BigDecimal(matcher.group());
            }
        } catch (Exception e) {
            log.warn("Failed to parse budget: {}", budgetStr, e);
        }
        return null;
    }

    /**
     * Builds response DTO from lead entity.
     */
    private LeadQualificationResponse buildResponse(Lead lead, LeadQualificationAgent.LeadExtractionResult extraction) {
        LeadQualificationResponse response = new LeadQualificationResponse();
        response.setLeadId(lead.getId());
        response.setName(lead.getName());
        response.setEmail(lead.getEmail());
        response.setPhone(lead.getPhone());
        response.setRequirementText(lead.getRequirementText());
        response.setBudget(extraction.getBudget()); // Return original string
        response.setUrgency(lead.getUrgency());
        response.setTimeline(lead.getTimeline());
        response.setLeadScore(lead.getLeadScore());
        response.setStatus(lead.getStatus());
        response.setSummary(lead.getSummary());
        return response;
    }

    /**
     * Updates conversation with lead contact information.
     * Preserves existing values when newly extracted field is null.
     */
    private void updateConversationContact(Conversation conversation, Lead lead) {
        boolean updated = false;

        // Update customerName if lead has a name and conversation doesn't
        if (lead.getName() != null && !lead.getName().isBlank()) {
            if (conversation.getCustomerName() == null || conversation.getCustomerName().isBlank() 
                    || conversation.getCustomerName().equals("Anonymous")) {
                conversation.setCustomerName(lead.getName());
                updated = true;
            }
        }

        // Update customerEmail if lead has email and conversation doesn't
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) {
            if (conversation.getCustomerEmail() == null || conversation.getCustomerEmail().isBlank()) {
                conversation.setCustomerEmail(lead.getEmail());
                updated = true;
            }
        }

        // Update customerPhone if lead has phone and conversation doesn't
        if (lead.getPhone() != null && !lead.getPhone().isBlank()) {
            if (conversation.getCustomerPhone() == null || conversation.getCustomerPhone().isBlank()) {
                conversation.setCustomerPhone(lead.getPhone());
                updated = true;
            }
        }

        if (updated) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
            log.info("Updated conversation {} with lead contact information", conversation.getId());
        }
    }

    /**
     * Logs agent action.
     */
    private void logAction(Business business, Conversation conversation, Lead lead, 
                          String action, String inputJson, String outputJson, 
                          AgentActionStatus status, String errorMessage, Long durationMs) {
        try {
            AgentLog agentLog = new AgentLog();
            agentLog.setAgentName("LeadQualificationAgent");
            agentLog.setAction(action);
            agentLog.setBusiness(business);
            agentLog.setConversation(conversation);
            agentLog.setLead(lead);
            agentLog.setInputJson(inputJson);
            agentLog.setOutputJson(outputJson);
            agentLog.setStatus(status);
            agentLog.setErrorMessage(errorMessage);
            agentLog.setDurationMs(durationMs);
            agentLogRepository.save(agentLog);
        } catch (Exception e) {
            log.error("Failed to log agent action", e);
        }
    }

    private String createInputJson(LeadQualificationRequest request) {
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("businessId", request.getBusinessId());
            input.put("conversationId", request.getConversationId());
            input.put("message", request.getMessage());
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
