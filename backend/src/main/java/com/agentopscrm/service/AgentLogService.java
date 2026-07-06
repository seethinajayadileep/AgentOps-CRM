package com.agentopscrm.service;

import com.agentopscrm.dto.AgentLogResponse;
import com.agentopscrm.dto.AgentLogSummaryResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.repository.AgentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for agent log operations.
 *
 * Why exists: Provides business logic for agent logs observability including
 * filtering, pagination, and summary statistics.
 *
 * @author AgentOps Team
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */
@Service
public class AgentLogService {

    private static final Logger logger = LoggerFactory.getLogger(AgentLogService.class);

    private final AgentLogRepository agentLogRepository;

    public AgentLogService(AgentLogRepository agentLogRepository) {
        this.agentLogRepository = agentLogRepository;
    }

    /**
     * Get all agent logs with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<AgentLogResponse> getAllAgentLogs(
        String search,
        String agentName,
        String action,
        AgentActionStatus status,
        UUID businessId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int page,
        int size,
        String sortBy
    ) {
        logger.info("Fetching agent logs with filters - agentName: {}, action: {}, status: {}", 
            agentName, action, status);

        // Build dynamic specification for filtering
        Specification<AgentLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search filter (agent name, action, or id)
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("agentName")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("action")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("id").as(String.class)), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // Agent name filter
            if (agentName != null && !agentName.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("agentName"), agentName));
            }

            // Action filter
            if (action != null && !action.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }

            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Business filter
            if (businessId != null) {
                predicates.add(criteriaBuilder.equal(root.get("business").get("id"), businessId));
            }

            // Date range filter
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Default sort: newest first
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortBy != null && !sortBy.isBlank()) {
            // Parse sortBy (e.g., "createdAt,desc" or "durationMs,asc")
            String[] sortParts = sortBy.split(",");
            String sortField = sortParts[0];
            Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
            sort = Sort.by(direction, sortField);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AgentLog> logsPage = agentLogRepository.findAll(spec, pageable);

        // Map to response DTOs
        return logsPage.map(this::mapToResponse);
    }

    /**
     * Get a specific agent log by ID.
     */
    @Transactional(readOnly = true)
    public AgentLogResponse getAgentLogById(UUID id) {
        logger.info("Fetching agent log: {}", id);
        AgentLog log = agentLogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agent log not found: " + id));
        return mapToResponse(log);
    }

    /**
     * Get summary statistics for agent logs.
     */
    @Transactional(readOnly = true)
    public AgentLogSummaryResponse getSummary() {
        logger.info("Calculating agent log summary statistics");

        // Calculate today's start and end
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // Count executions today
        long executionsToday = agentLogRepository.countByCreatedAtBetween(todayStart, todayEnd);

        // Count errors (ERROR and FAILED status)
        long errorCount = agentLogRepository.countByStatus(AgentActionStatus.ERROR) 
            + agentLogRepository.countByStatus(AgentActionStatus.FAILED);

        // Calculate success rate (all time)
        long totalCount = agentLogRepository.count();
        long successCount = agentLogRepository.countByStatus(AgentActionStatus.SUCCESS);
        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0.0;

        // Calculate average duration
        Long averageDuration = calculateAverageDuration();

        return new AgentLogSummaryResponse(executionsToday, successRate, errorCount, averageDuration);
    }

    /**
     * Calculate average duration across all logs.
     */
    private Long calculateAverageDuration() {
        List<AgentLog> recentLogs = agentLogRepository.findAll(
            PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        if (recentLogs.isEmpty()) {
            return 0L;
        }

        long totalDuration = recentLogs.stream()
            .filter(log -> log.getDurationMs() != null)
            .mapToLong(AgentLog::getDurationMs)
            .sum();

        long countWithDuration = recentLogs.stream()
            .filter(log -> log.getDurationMs() != null)
            .count();

        return countWithDuration > 0 ? totalDuration / countWithDuration : 0L;
    }

    /**
     * Map AgentLog entity to AgentLogResponse DTO.
     */
    private AgentLogResponse mapToResponse(AgentLog log) {
        AgentLogResponse response = new AgentLogResponse();
        response.setId(log.getId());
        response.setAgentName(log.getAgentName());
        response.setAction(log.getAction());
        response.setStatus(log.getStatus());
        response.setDurationMs(log.getDurationMs());
        response.setCreatedAt(log.getCreatedAt());
        response.setInputJson(log.getInputJson());
        response.setOutputJson(log.getOutputJson());
        response.setErrorMessage(log.getErrorMessage());

        // Map related entities (safely handle nulls)
        if (log.getBusiness() != null) {
            response.setBusinessId(log.getBusiness().getId());
            response.setBusinessName(log.getBusiness().getName());
        }
        if (log.getLead() != null) {
            response.setLeadId(log.getLead().getId());
            response.setLeadName(log.getLead().getName());
        }
        if (log.getConversation() != null) {
            response.setConversationId(log.getConversation().getId());
        }

        return response;
    }
}
