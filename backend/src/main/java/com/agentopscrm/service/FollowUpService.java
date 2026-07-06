package com.agentopscrm.service;

import com.agentopscrm.agent.FollowUpAgent;
import com.agentopscrm.dto.ApprovalResponse;
import com.agentopscrm.dto.FollowUpGenerateResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Approval;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating follow-up messages for qualified leads.
 * Coordinates between FollowUpAgent and ApprovalRepository.
 *
 * @author AgentOps Team
 * @version 0.7.0
 */
@Service
public class FollowUpService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);

    private final FollowUpAgent followUpAgent;
    private final LeadRepository leadRepository;
    private final ApprovalRepository approvalRepository;
    private final ConversationRepository conversationRepository;
    private final AgentLogRepository agentLogRepository;
    private final ObjectMapper objectMapper;

    public FollowUpService(
            FollowUpAgent followUpAgent,
            LeadRepository leadRepository,
            ApprovalRepository approvalRepository,
            ConversationRepository conversationRepository,
            AgentLogRepository agentLogRepository,
            ObjectMapper objectMapper) {
        this.followUpAgent = followUpAgent;
        this.leadRepository = leadRepository;
        this.approvalRepository = approvalRepository;
        this.conversationRepository = conversationRepository;
        this.agentLogRepository = agentLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates follow-up messages for a lead and creates approval records.
     *
     * @param leadId Lead ID
     * @param tone Tone preference (ALL generates all three styles)
     * @return FollowUpGenerateResponse containing approval records
     * @throws RuntimeException if lead not found or generation fails
     */
    @Transactional
    public FollowUpGenerateResponse generateFollowUpMessages(UUID leadId, String tone) {
        long startTime = System.currentTimeMillis();
        log.info("Generating follow-up messages for lead ID: {}", leadId);

        // Log generation started
        logAgentAction(leadId, null, "GENERATE_FOLLOWUP_STARTED",
                Map.of("leadId", leadId.toString(), "tone", tone),
                null, AgentActionStatus.SUCCESS, null, 0L);

        // Fetch lead
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with ID: " + leadId));

        Business business = lead.getBusiness();
        if (business == null) {
            throw new RuntimeException("Lead must be associated with a business");
        }

        // Get conversation summary if available
        String conversationSummary = getConversationSummary(lead);

        // Generate messages
        FollowUpAgent.FollowUpMessages messages;
        boolean usedFallback = false;
        
        try {
            messages = followUpAgent.generateFollowUpMessages(lead, business, conversationSummary);
        } catch (FollowUpAgent.FollowUpGenerationException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to generate follow-up messages, using fallback", e);
            logAgentAction(leadId, null, "GENERATE_FOLLOWUP_FAILED",
                    Map.of("leadId", leadId.toString(), "tone", tone),
                    null, AgentActionStatus.FAILED, e.getMessage(), duration);
            throw new RuntimeException("Failed to generate follow-up messages", e);
        }

        // Create approval records for each style
        List<Approval> approvals = new ArrayList<>();
        
        if ("ALL".equalsIgnoreCase(tone)) {
            approvals.add(createApproval(lead, business, "PROFESSIONAL", messages.getProfessional()));
            approvals.add(createApproval(lead, business, "FRIENDLY", messages.getFriendly()));
            approvals.add(createApproval(lead, business, "SHORT_WHATSAPP", messages.getShortWhatsapp()));
        } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
            approvals.add(createApproval(lead, business, "PROFESSIONAL", messages.getProfessional()));
        } else if ("FRIENDLY".equalsIgnoreCase(tone)) {
            approvals.add(createApproval(lead, business, "FRIENDLY", messages.getFriendly()));
        } else if ("SHORT_WHATSAPP".equalsIgnoreCase(tone)) {
            approvals.add(createApproval(lead, business, "SHORT_WHATSAPP", messages.getShortWhatsapp()));
        } else {
            // Default to ALL
            approvals.add(createApproval(lead, business, "PROFESSIONAL", messages.getProfessional()));
            approvals.add(createApproval(lead, business, "FRIENDLY", messages.getFriendly()));
            approvals.add(createApproval(lead, business, "SHORT_WHATSAPP", messages.getShortWhatsapp()));
        }

        // Save all approvals
        approvals = approvalRepository.saveAll(approvals);

        // Log approvals created
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("approvalsCreated", approvals.size());
        outputData.put("usedFallback", usedFallback);
        outputData.put("approvalIds", approvals.stream()
                .map(a -> a.getId().toString())
                .collect(Collectors.toList()));
        
        long duration = System.currentTimeMillis() - startTime;
        logAgentAction(leadId, null, "GENERATE_FOLLOWUP_COMPLETED",
                Map.of("leadId", leadId.toString(), "tone", tone),
                outputData, AgentActionStatus.SUCCESS, null, duration);

        // Convert to response
        List<ApprovalResponse> approvalResponses = approvals.stream()
                .map(this::toApprovalResponse)
                .collect(Collectors.toList());

        log.info("Successfully generated {} follow-up message(s) for lead ID: {}", approvals.size(), leadId);

        return FollowUpGenerateResponse.builder()
                .leadId(leadId)
                .approvals(approvalResponses)
                .build();
    }

    /**
     * Creates an Approval entity for a follow-up message.
     */
    private Approval createApproval(Lead lead, Business business, String style, String content) {
        Approval approval = new Approval();
        approval.setBusiness(business);
        approval.setLead(lead);
        approval.setApprovalType(ApprovalType.FOLLOW_UP_MESSAGE);
        approval.setStyle(style);
        approval.setContent(content);
        approval.setStatus(ApprovalStatus.PENDING);
        
        log.info("Creating approval for lead {} with style: {}", lead.getId(), style);
        
        // Log approval creation
        logAgentAction(lead.getId(), null, "APPROVAL_CREATED",
                Map.of("leadId", lead.getId().toString(), "style", style, "type", "FOLLOW_UP_MESSAGE"),
                Map.of("contentLength", content.length()),
                AgentActionStatus.SUCCESS, null, 0L);
        
        return approval;
    }

    /**
     * Gets conversation summary for a lead if available.
     */
    private String getConversationSummary(Lead lead) {
        if (lead.getConversation() != null) {
            Conversation conversation = conversationRepository.findById(lead.getConversation().getId())
                    .orElse(null);
            if (conversation != null && conversation.getSummary() != null) {
                return conversation.getSummary();
            }
        }
        return null;
    }

    /**
     * Converts Approval entity to ApprovalResponse DTO.
     */
    private ApprovalResponse toApprovalResponse(Approval approval) {
        return ApprovalResponse.builder()
                .approvalId(approval.getId())
                .type(approval.getApprovalType())
                .status(approval.getStatus())
                .style(approval.getStyle())
                .content(approval.getContent())
                .leadId(approval.getLead() != null ? approval.getLead().getId() : null)
                .leadName(approval.getLead() != null ? approval.getLead().getName() : null)
                .businessId(approval.getBusiness() != null ? approval.getBusiness().getId() : null)
                .businessName(approval.getBusiness() != null ? approval.getBusiness().getName() : null)
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .build();
    }

    /**
     * Logs an agent action to AgentLog.
     */
    private void logAgentAction(UUID leadId, UUID conversationId, String action,
                                Map<String, Object> inputData, Map<String, Object> outputData,
                                AgentActionStatus status, String errorMessage, Long durationMs) {
        try {
            AgentLog agentLog = new AgentLog();
            
            // Set lead if available
            if (leadId != null) {
                leadRepository.findById(leadId).ifPresent(agentLog::setLead);
                if (agentLog.getLead() != null) {
                    agentLog.setBusiness(agentLog.getLead().getBusiness());
                }
            }
            
            // Set conversation if available
            if (conversationId != null) {
                conversationRepository.findById(conversationId).ifPresent(agentLog::setConversation);
            }
            
            agentLog.setAgentName("FollowUpAgent");
            agentLog.setAction(action);
            agentLog.setInputJson(inputData != null ? objectMapper.writeValueAsString(inputData) : null);
            agentLog.setOutputJson(outputData != null ? objectMapper.writeValueAsString(outputData) : null);
            agentLog.setStatus(status);
            agentLog.setErrorMessage(errorMessage);
            agentLog.setDurationMs(durationMs);

            agentLogRepository.save(agentLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to log agent action", e);
        }
    }
}
