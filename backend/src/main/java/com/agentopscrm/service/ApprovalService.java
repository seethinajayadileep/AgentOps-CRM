package com.agentopscrm.service;

import com.agentopscrm.client.ResendClient;
import com.agentopscrm.dto.ApprovalResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Approval;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.ApprovalStatus;
import com.agentopscrm.entity.enums.ApprovalType;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.ApprovalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private final ResendClient resendClient;
    private final TransactionTemplate transactionTemplate;

    public ApprovalService(
            ApprovalRepository approvalRepository,
            AgentLogRepository agentLogRepository,
            ObjectMapper objectMapper,
            ResendClient resendClient,
            PlatformTransactionManager transactionManager) {
        this.approvalRepository = approvalRepository;
        this.agentLogRepository = agentLogRepository;
        this.objectMapper = objectMapper;
        this.resendClient = resendClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Approval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("approvalType"), type));
            }
            if (leadId != null) {
                predicates.add(cb.equal(root.get("lead").get("id"), leadId));
            }
            if (businessId != null) {
                predicates.add(cb.equal(root.get("business").get("id"), businessId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Approval> approvals = approvalRepository.findAll(spec, pageable).getContent();

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
     * Approves an approval. Email-style follow-ups are sent through Resend
     * when it is configured; the HTTP call happens outside the DB transaction.
     */
    public ApprovalResponse approveApproval(UUID id) {
        log.info("Approving approval ID: {}", id);

        SendPlan plan = transactionTemplate.execute(status -> loadSendPlan(id));
        if (plan == null) {
            throw new RuntimeException("Approval not found with ID: " + id);
        }

        if (plan.alreadySent || !plan.attemptSend) {
            return transactionTemplate.execute(status -> markApprovedOnly(id));
        }

        if (plan.toEmail == null) {
            return transactionTemplate.execute(status -> markSendBlocked(
                    id, "This lead has no email address. Add one before sending."));
        }

        long started = System.currentTimeMillis();
        try {
            ResendClient.SendResult sent = resendClient.sendEmail(plan.toEmail, plan.subject, plan.body);
            long duration = System.currentTimeMillis() - started;
            return transactionTemplate.execute(status -> markSent(id, plan.toEmail, plan.subject, sent.id(), duration));
        } catch (ResendClient.ResendException e) {
            long duration = System.currentTimeMillis() - started;
            String message = e.getMessage() == null ? "Failed to send email" : e.getMessage();
            log.warn("Resend send failed for approval {}: {}", id, message);
            return transactionTemplate.execute(status -> markSendFailed(id, message, duration));
        }
    }

    private SendPlan loadSendPlan(UUID id) {
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));
        Lead lead = approval.getLead();
        if (lead != null) {
            lead.getEmail();
            lead.getName();
        }
        if (approval.getBusiness() != null) {
            approval.getBusiness().getName();
        }

        boolean alreadySent = approval.getResendMessageId() != null && !approval.getResendMessageId().isBlank();
        boolean emailStyle = isEmailFollowUpStyle(approval);
        boolean retryable = approval.getStatus() == ApprovalStatus.PENDING
                || approval.getStatus() == ApprovalStatus.SEND_FAILED;
        boolean attemptSend = !alreadySent
                && emailStyle
                && retryable
                && resendClient.isConfigured();

        String toEmail = null;
        if (attemptSend) {
            toEmail = normalizeEmail(lead == null ? null : lead.getEmail());
        }

        String businessName = approval.getBusiness() != null ? approval.getBusiness().getName() : null;
        String subject = businessName == null || businessName.isBlank()
                ? "Follow-up"
                : "Follow-up from " + businessName.trim();

        return new SendPlan(alreadySent, attemptSend, toEmail, subject, approval.getContent());
    }

    private ApprovalResponse markApprovedOnly(UUID id) {
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));
        if (approval.getResendMessageId() != null && !approval.getResendMessageId().isBlank()) {
            return toApprovalResponse(approval);
        }
        if (approval.getStatus() != ApprovalStatus.PENDING && approval.getStatus() != ApprovalStatus.SEND_FAILED) {
            log.warn("Approval {} is not pending (current status: {})", id, approval.getStatus());
        }
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setSendError(null);
        approval = approvalRepository.save(approval);
        logApprovalAction(approval, "APPROVE", "Approval approved", AgentActionStatus.SUCCESS, null);
        return toApprovalResponse(approval);
    }

    private ApprovalResponse markSendBlocked(UUID id, String error) {
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setSendError(error);
        approval = approvalRepository.save(approval);
        logApprovalAction(approval, "FOLLOWUP_SEND_FAILED", error, AgentActionStatus.FAILED, null);
        return toApprovalResponse(approval);
    }

    private ApprovalResponse markSent(UUID id, String to, String subject, String messageId, long durationMs) {
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setSentTo(to);
        approval.setSentSubject(subject);
        approval.setResendMessageId(messageId);
        approval.setSentAt(LocalDateTime.now());
        approval.setSendError(null);
        approval = approvalRepository.save(approval);
        logApprovalAction(approval, "APPROVE", "Approval approved", AgentActionStatus.SUCCESS, durationMs);
        logApprovalAction(approval, "FOLLOWUP_SENT", "Sent via Resend", AgentActionStatus.SUCCESS, durationMs);
        return toApprovalResponse(approval);
    }

    private ApprovalResponse markSendFailed(UUID id, String error, long durationMs) {
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval not found with ID: " + id));
        approval.setStatus(ApprovalStatus.SEND_FAILED);
        approval.setSendError(error);
        approval = approvalRepository.save(approval);
        logApprovalAction(approval, "FOLLOWUP_SEND_FAILED", error, AgentActionStatus.FAILED, durationMs);
        return toApprovalResponse(approval);
    }

    static boolean isEmailFollowUpStyle(Approval approval) {
        if (approval.getApprovalType() != ApprovalType.FOLLOW_UP_MESSAGE) {
            return false;
        }
        String style = approval.getStyle();
        if (style == null) {
            return false;
        }
        String normalized = style.trim().toUpperCase(Locale.ROOT);
        return "PROFESSIONAL".equals(normalized) || "FRIENDLY".equals(normalized);
    }

    static String normalizeEmail(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.indexOf('@') < 1 || trimmed.contains(" ") || !trimmed.contains(".")) {
            return null;
        }
        return trimmed;
    }

    private record SendPlan(
            boolean alreadySent,
            boolean attemptSend,
            String toEmail,
            String subject,
            String body) {}

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
        logApprovalAction(approval, "REJECT", "Approval rejected", AgentActionStatus.SUCCESS, null);

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
                String.format("Status changed from %s to %s", oldStatus, newStatus),
                AgentActionStatus.SUCCESS, null);

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
                .leadEmail(approval.getLead() != null ? approval.getLead().getEmail() : null)
                .businessId(approval.getBusiness() != null ? approval.getBusiness().getId() : null)
                .businessName(approval.getBusiness() != null ? approval.getBusiness().getName() : null)
                .sentTo(approval.getSentTo())
                .sentSubject(approval.getSentSubject())
                .resendMessageId(approval.getResendMessageId())
                .sentAt(approval.getSentAt())
                .sendError(approval.getSendError())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .build();
    }

    private void logApprovalAction(
            Approval approval, String action, String message, AgentActionStatus status, Long durationMs) {
        try {
            Map<String, Object> inputJson = new HashMap<>();
            inputJson.put("approvalId", approval.getId().toString());
            inputJson.put("type", approval.getApprovalType());
            inputJson.put("status", approval.getStatus());
            if (approval.getSentTo() != null) {
                inputJson.put("sentTo", approval.getSentTo());
            }

            Map<String, Object> outputJson = new HashMap<>();
            outputJson.put("message", message);
            outputJson.put("timestamp", System.currentTimeMillis());
            if (approval.getResendMessageId() != null) {
                outputJson.put("resendMessageId", approval.getResendMessageId());
            }

            AgentLog agentLog = new AgentLog();
            agentLog.setBusiness(approval.getBusiness());
            agentLog.setLead(approval.getLead());
            agentLog.setConversation(null);
            agentLog.setAgentName("ApprovalService");
            agentLog.setAction(action);
            agentLog.setInputJson(objectMapper.writeValueAsString(inputJson));
            agentLog.setOutputJson(objectMapper.writeValueAsString(outputJson));
            agentLog.setStatus(status);
            agentLog.setDurationMs(durationMs);
            if (status != AgentActionStatus.SUCCESS) {
                agentLog.setErrorMessage(message);
            }

            agentLogRepository.save(agentLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to log approval action", e);
        }
    }
}
