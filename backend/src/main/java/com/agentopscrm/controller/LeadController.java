package com.agentopscrm.controller;

import com.agentopscrm.dto.LeadQualificationRequest;
import com.agentopscrm.dto.LeadQualificationResponse;
import com.agentopscrm.dto.LeadResponse;
import com.agentopscrm.dto.LeadStatusUpdateRequest;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.LeadStatus;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.service.LeadQualificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for lead management and qualification.
 *
 * @author AgentOps Team
 * @version 0.6.0
 */
@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*")
public class LeadController {

    private static final Logger log = LoggerFactory.getLogger(LeadController.class);

    private final LeadQualificationService qualificationService;
    private final LeadRepository leadRepository;

    public LeadController(LeadQualificationService qualificationService, 
                         LeadRepository leadRepository) {
        this.qualificationService = qualificationService;
        this.leadRepository = leadRepository;
    }

    /**
     * POST /api/leads/qualify - Qualify lead from customer message.
     */
    @PostMapping("/qualify")
    public ResponseEntity<LeadQualificationResponse> qualifyLead(
            @Valid @RequestBody LeadQualificationRequest request) {
        
        log.info("POST /api/leads/qualify - businessId: {}, conversationId: {}", 
                request.getBusinessId(), request.getConversationId());

        try {
            LeadQualificationResponse response = qualificationService.qualifyLead(request);
            log.info("Lead qualified successfully - leadId: {}, score: {}, status: {}", 
                    response.getLeadId(), response.getLeadScore(), response.getStatus());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to qualify lead", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/leads - Get all leads (newest first).
     */
    @GetMapping
    public ResponseEntity<List<LeadResponse>> getAllLeads() {
        log.info("GET /api/leads - fetching all leads");

        try {
            List<Lead> leads = leadRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            List<LeadResponse> responses = leads.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            
            log.info("Found {} leads", responses.size());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Failed to fetch leads", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/leads/{id} - Get single lead by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getLeadById(@PathVariable UUID id) {
        log.info("GET /api/leads/{} - fetching lead", id);

        try {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            
            LeadResponse response = toResponse(lead);
            log.info("Found lead: {}", lead.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Lead not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to fetch lead", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/leads/{id}/status - Update lead status.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<LeadResponse> updateLeadStatus(
            @PathVariable UUID id,
            @Valid @RequestBody LeadStatusUpdateRequest request) {
        
        log.info("PUT /api/leads/{}/status - updating to {}", id, request.getStatus());

        try {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            
            lead.setStatus(request.getStatus());
            lead = leadRepository.save(lead);

            LeadResponse response = toResponse(lead);
            log.info("Lead status updated successfully - leadId: {}, status: {}", 
                    lead.getId(), lead.getStatus());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Lead not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to update lead status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/leads/business/{businessId} - Get leads for a specific business.
     */
    @GetMapping("/business/{businessId}")
    public ResponseEntity<List<LeadResponse>> getLeadsByBusiness(@PathVariable UUID businessId) {
        log.info("GET /api/leads/business/{} - fetching leads for business", businessId);

        try {
            List<Lead> leads = leadRepository.findByBusinessId(businessId);
            List<LeadResponse> responses = leads.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            
            log.info("Found {} leads for business {}", responses.size(), businessId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Failed to fetch leads for business", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Converts Lead entity to LeadResponse DTO.
     */
    private LeadResponse toResponse(Lead lead) {
        LeadResponse response = new LeadResponse();
        response.setId(lead.getId());
        response.setBusinessId(lead.getBusiness() != null ? lead.getBusiness().getId() : null);
        response.setBusinessName(lead.getBusiness() != null ? lead.getBusiness().getName() : null);
        response.setConversationId(lead.getConversation() != null ? lead.getConversation().getId() : null);
        response.setName(lead.getName());
        response.setEmail(lead.getEmail());
        response.setPhone(lead.getPhone());
        response.setRequirementText(lead.getRequirementText());
        response.setBudget(lead.getBudget());
        response.setUrgency(lead.getUrgency());
        response.setTimeline(lead.getTimeline());
        response.setLeadScore(lead.getLeadScore() != null ? lead.getLeadScore().doubleValue() : null);
        response.setSummary(lead.getSummary());
        response.setStatus(lead.getStatus());
        response.setCreatedAt(lead.getCreatedAt());
        response.setUpdatedAt(lead.getUpdatedAt());
        return response;
    }
}
