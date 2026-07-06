package com.agentopscrm.service;

import com.agentopscrm.dto.ApprovalResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Approval;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing approval workflow.
 * Handles approval CRUD operations, status updates, and logging.
 *
 * @author AgentOps Team
 * @version 0.7.0
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRepository approvalRepository;
    private final AgentLogRepository agentLogRepository;
    private final ObjectMapper objectMapper;

    public ApprovalService(
            ApprovalRepository approvalRepository,
            AgentLogRepository agentLogRepository,
            ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.agentLogRepository = agentLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets all approvals with optional filters, sorted by newest first.
     *
     * @param status Optional status filter
     * @param type Optional type filter
     * @param leadId Optional lead ID filter
     * @param businessId Optional business ID filter
     * @return List of approval responses
     */
    @Transactional(readOnly = true)
    public List<ApprovalResponse> getAllApprovals(
            ApprovalStatus status,
            ApprovalType type,
            UUID leadId,
            UUID businessId) {
        
        log.info("Fetching approvals with filters: status={}, type={}, leadId={}, businessId={}", 
                status, type, leadId, businessId);

        List<Approval> approvals;
        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (status != null && type != null && leadId != null && businessId != null) {
            approvals = approvalRepository.findAllByBusinessIdAndLeadIdAndApprovalTypeAndStatus(
                    businessId, leadId, type, status, pageable).getContent();
        } else if (status != null && type != null && businessId != null) {
            approvals = approvalRepository.findAllByBusinessIdAndApprovalTypeAndStatus(
                    businessId, type, status, pageable).getContent();
        } else if (status != null && businessId != null) {
            approvals = approvalRepository.findAllByBusinessIdAndStatus(
                    businessId, status, pageable).getContent();
        } else if (type != null && businessId != null) {
            approvals = approvalRepository.findAllByBusinessIdAndApprovalType(
                    businessId, type, pageable).getContent();
        } else if (businessId != null) {
            approvals = approvalRepository.findAllByBusinessId(businessId, pageable).getContent();
        } else if (status != null) {
            approvals = approvalRepository.findByStatus(status, pageable).getContent();
        } else if (type != null) {
            approvals = approvalRepository.findByApprovalType(type, pageable).getContent();
        } else if (leadId != null) {
            approvals = approvalRepository.findAllByLeadId(leadId, pageable).getContent();
        } else {
            approvals = approvalRepository.findAll(pageable).getContent();
        }

        return approvals.stream()
                .map(this::toApprovalResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single approval by ID.
     *
     * @param id Approval ID
     * @return Approval response
     * @throws RuntimeException if approval not found
     */
    @Transactional(readOnly = true)
    public ApprovalResponse getApprovalById(UUID id) {
        log.info("Fetching approval by ID: {}", id);
        
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));
        
        return toApprovalResponse(approval);
    }

    /**
     * Approves an approval.
     *
     * @param id Approval ID
     * @return Updated approval response
     */
    @Transactional
    public ApprovalResponse approveApproval(UUID id) {
        log.info("Approving approval ID: {}", id);

        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            log.warn("Approval {} is not pending (current status: {})", id, approval.getStatus());
        }

        approval.setStatus(ApprovalStatus.APPROVED);
        approval = approvalRepository.save(approval);

        // Log the approval
        logApprovalAction(approval, "APPROVE", "Approval approved");

        log.info("Successfully approved approval ID: {}", id);
        return toApprovalResponse(approval);
    }

    /**
     * Rejects an approval.
     *
     * @param id Approval ID
     * @return Updated approval response
     */
    @Transactional
    public ApprovalResponse rejectApproval(UUID id) {
        log.info("Rejecting approval ID: {}", id);

        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            log.warn("Approval {} is not pending (current status: {})", id, approval.getStatus());
        }

        approval.setStatus(ApprovalStatus.REJECTED);
        approval = approvalRepository.save(approval);

        // Log the rejection
        logApprovalAction(approval, "REJECT", "Approval rejected");

        log.info("Successfully rejected approval ID: {}", id);
        return toApprovalResponse(approval);
    }

    /**
     * Updates approval status.
     *
     * @param id Approval ID
     * @param newStatus New status
     * @return Updated approval response
     */
    @Transactional
    public ApprovalResponse updateApprovalStatus(UUID id, ApprovalStatus newStatus) {
        log.info("Updating approval {} status to: {}", id, newStatus);

        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));

        ApprovalStatus oldStatus = approval.getStatus();
        approval.setStatus(newStatus);
        approval = approvalRepository.save(approval);

        // Log the status change
        logApprovalAction(approval, "STATUS_UPDATE", 
                String.format("Status changed from %s to %s", oldStatus, newStatus));

        log.info("Successfully updated approval {} status to: {}", id, newStatus);
        return toApprovalResponse(approval);
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
     * Logs an approval action to AgentLog.
     */
    private void logApprovalAction(Approval approval, String action, String message) {
        try {
            Map<String, Object> inputJson = new HashMap<>();
            inputJson.put("approvalId", approval.getId().toString());
            inputJson.put("type", approval.getApprovalType());
            inputJson.put("status", approval.getStatus());

            Map<String, Object> outputJson = new HashMap<>();
            outputJson.put("message", message);
            outputJson.put("timestamp", System.currentTimeMillis());

            AgentLog agentLog = new AgentLog();
            agentLog.setBusiness(approval.getBusiness());
            agentLog.setLead(approval.getLead());
            agentLog.setConversation(null);
            agentLog.setAgentName("ApprovalService");
            agentLog.setAction(action);
            agentLog.setInputJson(objectMapper.writeValueAsString(inputJson));
            agentLog.setOutputJson(objectMapper.writeValueAsString(outputJson));
            agentLog.setStatus(AgentActionStatus.SUCCESS);

            agentLogRepository.save(agentLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to log approval action", e);
        }
    }
}
