package com.agentopscrm.controller;

import com.agentopscrm.dto.ApprovalResponse;
import com.agentopscrm.dto.ApprovalStatusUpdateRequest;
import com.agentopscrm.dto.FollowUpGenerateRequest;
import com.agentopscrm.dto.FollowUpGenerateResponse;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.service.ApprovalService;
import com.agentopscrm.service.FollowUpService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for approval management and follow-up message generation.
 *
 * @author AgentOps Team
 * @version 0.7.0
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    private final FollowUpService followUpService;
    private final ApprovalService approvalService;

    public ApprovalController(FollowUpService followUpService, 
                             ApprovalService approvalService) {
        this.followUpService = followUpService;
        this.approvalService = approvalService;
    }

    /**
     * POST /api/leads/{leadId}/follow-up/generate - Generate follow-up messages for a lead.
     */
    @PostMapping("/leads/{leadId}/follow-up/generate")
    public ResponseEntity<FollowUpGenerateResponse> generateFollowUp(
            @PathVariable UUID leadId,
            @Valid @RequestBody FollowUpGenerateRequest request) {
        
        log.info("POST /api/leads/{}/follow-up/generate - tone: {}", leadId, request.getTone());

        try {
            FollowUpGenerateResponse response = followUpService.generateFollowUpMessages(
                    leadId, request.getTone());
            
            log.info("Follow-up messages generated successfully - leadId: {}, count: {}", 
                    leadId, response.getApprovals().size());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to generate follow-up messages for lead {}", leadId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/approvals - List all approvals with optional filters.
     */
    @GetMapping("/approvals")
    public ResponseEntity<List<ApprovalResponse>> getAllApprovals(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) ApprovalType type,
            @RequestParam(required = false) UUID leadId,
            @RequestParam(required = false) UUID businessId) {
        
        log.info("GET /api/approvals - status: {}, type: {}, leadId: {}, businessId: {}", 
                status, type, leadId, businessId);

        try {
            List<ApprovalResponse> approvals = approvalService.getAllApprovals(
                    status, type, leadId, businessId);
            
            log.info("Retrieved {} approvals", approvals.size());
            return ResponseEntity.ok(approvals);
        } catch (Exception e) {
            log.error("Failed to retrieve approvals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/approvals/{id} - Get single approval by ID.
     */
    @GetMapping("/approvals/{id}")
    public ResponseEntity<ApprovalResponse> getApprovalById(@PathVariable UUID id) {
        log.info("GET /api/approvals/{}", id);

        try {
            ApprovalResponse approval = approvalService.getApprovalById(id);
            return ResponseEntity.ok(approval);
        } catch (RuntimeException e) {
            log.error("Approval not found: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * PUT /api/approvals/{id}/approve - Approve an approval.
     */
    @PutMapping("/approvals/{id}/approve")
    public ResponseEntity<ApprovalResponse> approveApproval(@PathVariable UUID id) {
        log.info("PUT /api/approvals/{}/approve", id);

        try {
            ApprovalResponse approval = approvalService.approveApproval(id);
            log.info("Approval {} approved successfully", id);
            return ResponseEntity.ok(approval);
        } catch (RuntimeException e) {
            log.error("Failed to approve approval {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * PUT /api/approvals/{id}/reject - Reject an approval.
     */
    @PutMapping("/approvals/{id}/reject")
    public ResponseEntity<ApprovalResponse> rejectApproval(@PathVariable UUID id) {
        log.info("PUT /api/approvals/{}/reject", id);

        try {
            ApprovalResponse approval = approvalService.rejectApproval(id);
            log.info("Approval {} rejected successfully", id);
            return ResponseEntity.ok(approval);
        } catch (RuntimeException e) {
            log.error("Failed to reject approval {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * PUT /api/approvals/{id}/status - Update approval status.
     */
    @PutMapping("/approvals/{id}/status")
    public ResponseEntity<ApprovalResponse> updateApprovalStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalStatusUpdateRequest request) {
        
        log.info("PUT /api/approvals/{}/status - newStatus: {}", id, request.getStatus());

        try {
            ApprovalResponse approval = approvalService.updateApprovalStatus(
                    id, request.getStatus());
            log.info("Approval {} status updated to {}", id, request.getStatus());
            return ResponseEntity.ok(approval);
        } catch (RuntimeException e) {
            log.error("Failed to update approval {} status", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
