package com.agentopscrm.service;

import com.agentopscrm.dto.*;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.Message;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.Channel;
import com.agentopscrm.entity.enums.ConversationStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing conversations.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    private static final int MAX_PREVIEW_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 200;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BusinessRepository businessRepository;
    private final AgentLogRepository agentLogRepository;
    private final EntityManager entityManager;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            BusinessRepository businessRepository,
            AgentLogRepository agentLogRepository,
            EntityManager entityManager) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.businessRepository = businessRepository;
        this.agentLogRepository = agentLogRepository;
        this.entityManager = entityManager;
    }

    /**
     * Get paginated list of conversations with filtering.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ConversationListItemResponse> getAllConversations(
            String search,
            UUID businessId,
            ConversationStatus status,
            Channel channel,
            String leadCaptureStatus,
            String startDateStr,
            String endDateStr,
            int page,
            int size,
            String sortStr) {

        logger.info("Fetching conversations - page: {}, size: {}, search: {}, businessId: {}, status: {}, channel: {}, leadCaptureStatus: {}",
                page, size, search, businessId, status, channel, leadCaptureStatus);

        // Cap page size
        size = Math.min(size, MAX_PAGE_SIZE);
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (search != null && search.length() > MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException("search must be " + MAX_SEARCH_LENGTH + " characters or fewer");
        }

        LocalDateTime startDate = parseIsoDate(startDateStr, true);
        LocalDateTime endDate = parseIsoDate(endDateStr, false);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Conversation> criteriaQuery = cb.createQuery(Conversation.class);
        Root<Conversation> root = criteriaQuery.from(Conversation.class);

        List<Predicate> predicates = buildConversationPredicates(
                cb, root, search, businessId, status, channel, leadCaptureStatus, startDate, endDate, true);
        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        Sort.Direction direction = Sort.Direction.DESC;
        String sortProperty = "updatedAt";
        if (sortStr != null && !sortStr.trim().isEmpty()) {
            if (sortStr.toLowerCase().contains("asc")) {
                direction = Sort.Direction.ASC;
            }
            if (sortStr.toLowerCase().contains("created")) {
                sortProperty = "createdAt";
            }
        }

        if (direction == Sort.Direction.ASC) {
            criteriaQuery.orderBy(cb.asc(root.get(sortProperty)), cb.asc(root.get("createdAt")));
        } else {
            criteriaQuery.orderBy(cb.desc(root.get(sortProperty)), cb.desc(root.get("createdAt")));
        }

        TypedQuery<Conversation> query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<Conversation> conversations = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Conversation> countRoot = countQuery.from(Conversation.class);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPredicates = buildConversationPredicates(
                cb, countRoot, search, businessId, status, channel, leadCaptureStatus, startDate, endDate, false);
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Map to DTO
        List<ConversationListItemResponse> items = conversations.stream()
                .map(this::mapToListItemResponse)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) total / size);
        PaginationMeta pagination = new PaginationMeta(page, size, total, totalPages);
        return new PaginatedResponse<>(items, pagination);
    }

    private LocalDateTime parseIsoDate(String value, boolean startOfDay) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date. Use ISO format YYYY-MM-DD.");
        }
    }

    private List<Predicate> buildConversationPredicates(
            CriteriaBuilder cb,
            Root<Conversation> root,
            String search,
            UUID businessId,
            ConversationStatus status,
            Channel channel,
            String leadCaptureStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean fetchBusiness) {
        List<Predicate> predicates = new ArrayList<>();
        Join<Conversation, Business> businessJoin;
        if (fetchBusiness) {
            @SuppressWarnings("unchecked")
            Join<Conversation, Business> fetched =
                    (Join<Conversation, Business>) (Join<?, ?>) root.fetch("business", JoinType.LEFT);
            businessJoin = fetched;
        } else {
            businessJoin = root.join("business", JoinType.LEFT);
        }

        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("customerName"), "")), pattern, '\\'),
                    cb.like(cb.lower(cb.coalesce(root.get("customerEmail"), "")), pattern, '\\'),
                    cb.like(cb.lower(cb.coalesce(root.get("customerPhone"), "")), pattern, '\\'),
                    cb.like(cb.lower(cb.coalesce(businessJoin.get("name"), "")), pattern, '\\'),
                    cb.like(cb.lower(cb.coalesce(root.get("summary"), "")), pattern, '\\')
            ));
        }

        if (businessId != null) {
            predicates.add(cb.equal(businessJoin.get("id"), businessId));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (channel != null) {
            predicates.add(cb.equal(root.get("channel"), channel));
        }
        if (leadCaptureStatus != null && !leadCaptureStatus.trim().isEmpty()) {
            if (leadCaptureStatus.equalsIgnoreCase("null") || leadCaptureStatus.equalsIgnoreCase("none")) {
                predicates.add(cb.isNull(root.get("leadCaptureStatus")));
            } else {
                predicates.add(cb.equal(root.get("leadCaptureStatus"), leadCaptureStatus));
            }
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }
        return predicates;
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Get conversation details by ID.
     */
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetails(UUID conversationId) {
        logger.info("Fetching conversation details for ID: {}", conversationId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        return mapToDetailResponse(conversation);
    }

    /**
     * Get messages for a conversation with pagination.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ConversationMessageResponse> getConversationMessages(
            UUID conversationId,
            int page,
            int size) {

        logger.info("Fetching messages for conversation: {}, page: {}, size: {}", conversationId, page, size);

        // Verify conversation exists
        if (!conversationRepository.existsById(conversationId)) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }

        // Cap page size
        size = Math.min(size, MAX_PAGE_SIZE);

        // Fetch messages chronologically (oldest first for display)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);

        List<ConversationMessageResponse> items = messagePage.getContent().stream()
                .map(m -> new ConversationMessageResponse(
                        m.getId(),
                        m.getRole().toString(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .collect(Collectors.toList());

        int totalPages = messagePage.getTotalPages();
        PaginationMeta pagination = new PaginationMeta(
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                totalPages
        );

        return new PaginatedResponse<>(items, pagination);
    }

    /**
     * Update conversation status with validation and audit logging.
     */
    @Transactional
    public ConversationDetailResponse updateConversationStatus(
            UUID conversationId,
            ConversationStatus newStatus) {

        logger.info("Updating conversation {} status to {}", conversationId, newStatus);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        ConversationStatus oldStatus = conversation.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        // Update status
        conversation.setStatus(newStatus);
        Conversation updated = conversationRepository.save(conversation);

        // Log the status change (non-blocking)
        try {
            logStatusChange(conversation, oldStatus, newStatus);
        } catch (Exception e) {
            logger.error("Failed to log status change for conversation {}: {}", conversationId, e.getMessage());
            // Don't fail the main operation if logging fails
        }

        logger.info("Conversation {} status updated from {} to {}", conversationId, oldStatus, newStatus);
        return mapToDetailResponse(updated);
    }

    /**
     * Get conversation summary statistics.
     */
    @Transactional(readOnly = true)
    public ConversationSummaryResponse getConversationSummary() {
        logger.info("Fetching conversation summary statistics");

        long totalConversations = conversationRepository.count();
        long activeConversations = conversationRepository.countByStatus(ConversationStatus.ACTIVE);

        // Conversations created today
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Conversation> root = countQuery.from(Conversation.class);
        countQuery.select(cb.count(root));
        countQuery.where(cb.between(root.get("createdAt"), startOfDay, endOfDay));
        long conversationsToday = entityManager.createQuery(countQuery).getSingleResult();

        // Count conversations where leadCaptureStatus = 'CAPTURED'
        CriteriaQuery<Long> capturedQuery = cb.createQuery(Long.class);
        Root<Conversation> capturedRoot = capturedQuery.from(Conversation.class);
        capturedQuery.select(cb.count(capturedRoot));
        capturedQuery.where(cb.equal(capturedRoot.get("leadCaptureStatus"), "CAPTURED"));
        long leadsCaptured = entityManager.createQuery(capturedQuery).getSingleResult();

        // Average messages per conversation
        double avgMessages = 0.0;
        if (totalConversations > 0) {
            long totalMessages = messageRepository.count();
            avgMessages = Math.round((double) totalMessages / totalConversations * 10.0) / 10.0;
        }

        return new ConversationSummaryResponse(
                totalConversations,
                activeConversations,
                conversationsToday,
                leadsCaptured,
                avgMessages
        );
    }

    /**
     * Map Conversation to ConversationListItemResponse.
     * Efficiently calculates preview and counts without N+1.
     */
    private ConversationListItemResponse mapToListItemResponse(Conversation c) {
        ConversationListItemResponse dto = new ConversationListItemResponse();
        dto.setId(c.getId());
        dto.setBusinessId(c.getBusiness().getId());
        dto.setBusinessName(c.getBusiness().getName());
        dto.setCustomerName(c.getCustomerName());
        dto.setCustomerEmail(c.getCustomerEmail());
        dto.setCustomerPhone(c.getCustomerPhone());
        dto.setChannel(c.getChannel().toString());
        dto.setStatus(c.getStatus().toString());
        dto.setSummary(c.getSummary());
        dto.setLeadCaptureStatus(c.getLeadCaptureStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());

        // Count messages and leads (uses indexed queries)
        dto.setMessageCount((int) messageRepository.countByConversationId(c.getId()));
        dto.setLeadCount(c.getLeads() != null ? c.getLeads().size() : 0);

        // Get latest message for preview (efficient query)
        List<Message> latestMessages = messageRepository
                .findLatestMessages(c.getId(), PageRequest.of(0, 1))
                .getContent();

        if (!latestMessages.isEmpty()) {
            Message latest = latestMessages.get(0);
            dto.setLatestMessageRole(latest.getRole().toString());
            dto.setLatestMessageAt(latest.getCreatedAt());

            String content = latest.getContent();
            if (content != null && content.length() > MAX_PREVIEW_LENGTH) {
                dto.setLatestMessagePreview(content.substring(0, MAX_PREVIEW_LENGTH) + "...");
            } else {
                dto.setLatestMessagePreview(content);
            }
        }

        return dto;
    }

    /**
     * Map Conversation to ConversationDetailResponse.
     */
    private ConversationDetailResponse mapToDetailResponse(Conversation c) {
        ConversationDetailResponse dto = new ConversationDetailResponse();
        dto.setId(c.getId());
        dto.setBusinessId(c.getBusiness().getId());
        dto.setBusinessName(c.getBusiness().getName());
        dto.setCustomerName(c.getCustomerName());
        dto.setCustomerEmail(c.getCustomerEmail());
        dto.setCustomerPhone(c.getCustomerPhone());
        dto.setChannel(c.getChannel().toString());
        dto.setStatus(c.getStatus().toString());
        dto.setSummary(c.getSummary());
        dto.setLeadCaptureStatus(c.getLeadCaptureStatus());
        dto.setPendingLeadName(c.getPendingLeadName());
        dto.setPendingLeadEmail(c.getPendingLeadEmail());
        dto.setPendingLeadPhone(c.getPendingLeadPhone());
        dto.setPendingLeadRequirement(c.getPendingLeadRequirement());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());

        // Related leads (minimal info)
        List<ConversationDetailResponse.RelatedLead> relatedLeads = new ArrayList<>();
        if (c.getLeads() != null) {
            for (Lead lead : c.getLeads()) {
                ConversationDetailResponse.RelatedLead relatedLead = new ConversationDetailResponse.RelatedLead();
                relatedLead.setId(lead.getId());
                relatedLead.setName(lead.getName());
                relatedLead.setEmail(lead.getEmail());
                relatedLead.setStatus(lead.getStatus() != null ? lead.getStatus().toString() : null);
                // Convert Double to Integer for leadScore
                relatedLead.setLeadScore(lead.getLeadScore() != null ? lead.getLeadScore().intValue() : null);
                relatedLeads.add(relatedLead);
            }
        }
        dto.setRelatedLeads(relatedLeads);

        // Voice call count
        dto.setVoiceCallCount(c.getVoiceCalls() != null ? c.getVoiceCalls().size() : 0);

        return dto;
    }

    /**
     * Validate status transition rules.
     */
    private void validateStatusTransition(ConversationStatus from, ConversationStatus to) {
        if (from == to) {
            return; // No change
        }

        // Valid transitions (per spec):
        // ACTIVE → PAUSED, CLOSED, ARCHIVED
        // PAUSED → ACTIVE, CLOSED, ARCHIVED
        // CLOSED → ACTIVE, ARCHIVED
        // ARCHIVED → ACTIVE (if restoration is allowed)

        boolean valid = switch (from) {
            case ACTIVE -> to == ConversationStatus.PAUSED || to == ConversationStatus.CLOSED || to == ConversationStatus.ARCHIVED;
            case PAUSED -> to == ConversationStatus.ACTIVE || to == ConversationStatus.CLOSED || to == ConversationStatus.ARCHIVED;
            case CLOSED -> to == ConversationStatus.ACTIVE || to == ConversationStatus.ARCHIVED;
            case ARCHIVED -> to == ConversationStatus.ACTIVE; // Allow restoration
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", from, to)
            );
        }
    }

    /**
     * Log status change to AgentLog for audit trail.
     */
    private void logStatusChange(Conversation conversation, ConversationStatus oldStatus, ConversationStatus newStatus) {
        try {
            AgentLog log = new AgentLog();
            log.setAgentName("ConversationManager");
            log.setAction("CONVERSATION_STATUS_CHANGED");
            log.setStatus(AgentActionStatus.SUCCESS);
            log.setInputJson(String.format("{\"conversationId\":\"%s\",\"oldStatus\":\"%s\",\"requestedStatus\":\"%s\"}",
                    conversation.getId(), oldStatus, newStatus));
            log.setOutputJson(String.format("{\"finalStatus\":\"%s\"}", newStatus));
            log.setConversation(conversation);
            log.setBusiness(conversation.getBusiness());

            agentLogRepository.save(log);
            logger.info("Logged status change for conversation {}", conversation.getId());
        } catch (Exception e) {
            logger.error("Error logging status change: {}", e.getMessage());
            // Don't propagate the exception
        }
    }
}
