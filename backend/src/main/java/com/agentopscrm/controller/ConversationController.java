package com.agentopscrm.controller;

import com.agentopscrm.dto.*;
import com.agentopscrm.entity.enums.Channel;
import com.agentopscrm.entity.enums.ConversationStatus;
import com.agentopscrm.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for conversation management (admin inbox).
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * GET /api/conversations
     * List all conversations with filtering and pagination.
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ConversationListItemResponse>> getAllConversations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID businessId,
            @RequestParam(required = false) ConversationStatus status,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) String leadCaptureStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        logger.info("GET /api/conversations - page: {}, size: {}, filters: search={}, businessId={}, status={}, channel={}, leadCaptureStatus={}",
                page, size, search, businessId, status, channel, leadCaptureStatus);

        try {
            PaginatedResponse<ConversationListItemResponse> response = conversationService.getAllConversations(
                    search, businessId, status, channel, leadCaptureStatus,
                    startDate, endDate, page, size, sort
            );

            logger.info("Retrieved {} conversations, total: {}",
                    response.getItems().size(), response.getPagination().getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching conversations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/conversations/{id}
     * Get detailed conversation information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversationDetails(@PathVariable UUID id) {
        logger.info("GET /api/conversations/{}", id);

        try {
            ConversationDetailResponse response = conversationService.getConversationDetails(id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Conversation not found: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error fetching conversation details", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/conversations/{id}/messages
     * Get messages for a conversation with pagination.
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<PaginatedResponse<ConversationMessageResponse>> getConversationMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        logger.info("GET /api/conversations/{}/messages - page: {}, size: {}", id, page, size);

        try {
            PaginatedResponse<ConversationMessageResponse> response =
                    conversationService.getConversationMessages(id, page, size);

            logger.info("Retrieved {} messages for conversation {}", response.getItems().size(), id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Conversation not found: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error fetching conversation messages", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PATCH /api/conversations/{id}/status
     * Update conversation status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ConversationDetailResponse> updateConversationStatus(
            @PathVariable UUID id,
            @RequestBody ConversationStatusUpdateRequest request) {

        logger.info("PATCH /api/conversations/{}/status - new status: {}", id, request.getStatus());

        if (request.getStatus() == null) {
            logger.error("Status is required");
            return ResponseEntity.badRequest().build();
        }

        try {
            ConversationDetailResponse response = conversationService.updateConversationStatus(
                    id,
                    request.getStatus()
            );

            logger.info("Conversation {} status updated to {}", id, request.getStatus());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid status update request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("Error updating conversation status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/conversations/summary
     * Get conversation summary statistics.
     */
    @GetMapping("/summary")
    public ResponseEntity<ConversationSummaryResponse> getConversationSummary() {
        logger.info("GET /api/conversations/summary");

        try {
            ConversationSummaryResponse response = conversationService.getConversationSummary();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching conversation summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
