package com.agentopscrm.controller;

import com.agentopscrm.dto.AgentLogResponse;
import com.agentopscrm.dto.AgentLogSummaryResponse;
import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.dto.PaginatedResponse;
import com.agentopscrm.dto.PaginationMeta;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.service.AgentLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for agent log operations.
 *
 * Why exists: Provides HTTP endpoints for agent logs observability including
 * filtering, pagination, and summary statistics.
 *
 * @author AgentOps Team
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */
@RestController
@RequestMapping("/api/agent-logs")
public class AgentLogController {

    private static final Logger logger = LoggerFactory.getLogger(AgentLogController.class);

    private final AgentLogService agentLogService;

    public AgentLogController(AgentLogService agentLogService) {
        this.agentLogService = agentLogService;
    }

    /**
     * Get all agent logs with filtering and pagination.
     *
     * GET /api/agent-logs
     *
     * Query parameters:
     * - search: Search by execution ID, agent name, or action
     * - agentName: Filter by agent name
     * - action: Filter by action
     * - status: Filter by status (SUCCESS, PARTIAL, ERROR, FAILED, FALLBACK_USED)
     * - businessId: Filter by business ID
     * - startDate: Filter by start date (ISO 8601 format)
     * - endDate: Filter by end date (ISO 8601 format)
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - sort: Sort field and direction (e.g., "createdAt,desc")
     *
     * @return paginated list of agent logs
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<AgentLogResponse>> getAllAgentLogs(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String agentName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) AgentActionStatus status,
        @RequestParam(required = false) UUID businessId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sort
    ) {
        logger.info("Fetching agent logs - page: {}, size: {}, filters applied: {}", 
            page, size, (search != null || agentName != null || action != null || status != null));

        try {
            Page<AgentLogResponse> logsPage = agentLogService.getAllAgentLogs(
                search, agentName, action, status, businessId, startDate, endDate, page, size, sort
            );

            PaginationMeta meta = new PaginationMeta(
                logsPage.getNumber(),
                logsPage.getSize(),
                logsPage.getTotalElements(),
                logsPage.getTotalPages()
            );

            PaginatedResponse<AgentLogResponse> response = new PaginatedResponse<>(
                logsPage.getContent(), 
                meta
            );
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch agent logs", e);
            PaginatedResponse<AgentLogResponse> errorResponse = new PaginatedResponse<>(null, null);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Get a specific agent log by ID.
     *
     * GET /api/agent-logs/{id}
     *
     * @param id the agent log ID
     * @return the agent log details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentLogResponse>> getAgentLogById(@PathVariable UUID id) {
        logger.info("Fetching agent log: {}", id);

        try {
            AgentLogResponse response = agentLogService.getAgentLogById(id);
            return ResponseEntity.ok(
                ApiResponse.success(response, "Agent log retrieved successfully")
            );

        } catch (IllegalArgumentException e) {
            logger.error("Agent log not found: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to fetch agent log", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch agent log: " + e.getMessage()));
        }
    }

    /**
     * Get summary statistics for agent logs.
     *
     * GET /api/agent-logs/summary
     *
     * Returns:
     * - executionsToday: Number of agent executions today
     * - successRate: Overall success rate percentage
     * - errorCount: Total number of errors (ERROR + FAILED status)
     * - averageDurationMs: Average execution duration in milliseconds
     *
     * @return summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AgentLogSummaryResponse>> getSummary() {
        logger.info("Fetching agent log summary statistics");

        try {
            AgentLogSummaryResponse summary = agentLogService.getSummary();
            return ResponseEntity.ok(
                ApiResponse.success(summary, "Summary retrieved successfully")
            );

        } catch (Exception e) {
            logger.error("Failed to fetch agent log summary", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch summary: " + e.getMessage()));
        }
    }
}
