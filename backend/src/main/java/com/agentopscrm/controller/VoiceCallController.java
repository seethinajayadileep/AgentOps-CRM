package com.agentopscrm.controller;

import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.dto.PaginatedResponse;
import com.agentopscrm.dto.PaginationMeta;
import com.agentopscrm.dto.VoiceCallResponse;
import com.agentopscrm.dto.VoiceCallStartRequest;
import com.agentopscrm.service.VoiceCallService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for voice call operations.
 * 
 * Why exists: Provides HTTP endpoints for starting voice calls and retrieving call history.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
@RestController
@RequestMapping("/api")
public class VoiceCallController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCallController.class);

    private final VoiceCallService voiceCallService;

    public VoiceCallController(VoiceCallService voiceCallService) {
        this.voiceCallService = voiceCallService;
    }

    /**
     * Start a new voice call for a lead.
     * 
     * POST /api/leads/{leadId}/voice-calls/start
     *
     * @param leadId the ID of the lead
     * @param request the call start request
     * @return the created voice call
     */
    @PostMapping("/leads/{leadId}/voice-calls/start")
    public ResponseEntity<ApiResponse<VoiceCallResponse>> startVoiceCall(
        @PathVariable UUID leadId,
        @Valid @RequestBody VoiceCallStartRequest request
    ) {
        logger.info("Received request to start voice call for lead: {}", leadId);

        try {
            VoiceCallResponse response = voiceCallService.startCall(leadId, request);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Voice call started successfully"));

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to start voice call", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to start voice call: " + e.getMessage()));
        }
    }

    /**
     * Get all voice calls for a lead.
     * 
     * GET /api/leads/{leadId}/voice-calls
     *
     * @param leadId the ID of the lead
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paginated list of voice calls
     */
    @GetMapping("/leads/{leadId}/voice-calls")
    public ResponseEntity<PaginatedResponse<VoiceCallResponse>> getLeadVoiceCalls(
        @PathVariable UUID leadId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Fetching voice calls for lead: {}", leadId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<VoiceCallResponse> callsPage = voiceCallService.getCallsByLead(leadId, pageable);

            PaginationMeta meta = new PaginationMeta(
                callsPage.getNumber(),
                callsPage.getSize(),
                callsPage.getTotalElements(),
                callsPage.getTotalPages()
            );

            PaginatedResponse<VoiceCallResponse> response = new PaginatedResponse<>(callsPage.getContent(), meta);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch voice calls for lead", e);
            PaginatedResponse<VoiceCallResponse> errorResponse = new PaginatedResponse<>(null, null);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Get all voice calls for a business.
     * 
     * GET /api/businesses/{businessId}/voice-calls
     *
     * @param businessId the ID of the business
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paginated list of voice calls
     */
    @GetMapping("/businesses/{businessId}/voice-calls")
    public ResponseEntity<PaginatedResponse<VoiceCallResponse>> getBusinessVoiceCalls(
        @PathVariable UUID businessId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Fetching voice calls for business: {}", businessId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<VoiceCallResponse> callsPage = voiceCallService.getCallsByBusiness(businessId, pageable);

            PaginationMeta meta = new PaginationMeta(
                callsPage.getNumber(),
                callsPage.getSize(),
                callsPage.getTotalElements(),
                callsPage.getTotalPages()
            );

            PaginatedResponse<VoiceCallResponse> response = new PaginatedResponse<>(callsPage.getContent(), meta);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch voice calls for business", e);
            PaginatedResponse<VoiceCallResponse> errorResponse = new PaginatedResponse<>(null, null);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Get all voice calls across every business.
     *
     * GET /api/voice-calls
     *
     * Used by the global Voice Calls page which is not scoped to a single
     * business, so newly-created calls always appear regardless of which
     * business/lead they belong to.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paginated list of voice calls
     */
    @GetMapping("/voice-calls")
    public ResponseEntity<PaginatedResponse<VoiceCallResponse>> getAllVoiceCalls(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Fetching all voice calls (page={}, size={})", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<VoiceCallResponse> callsPage = voiceCallService.getAllCalls(pageable);

            PaginationMeta meta = new PaginationMeta(
                callsPage.getNumber(),
                callsPage.getSize(),
                callsPage.getTotalElements(),
                callsPage.getTotalPages()
            );

            PaginatedResponse<VoiceCallResponse> response = new PaginatedResponse<>(callsPage.getContent(), meta);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch all voice calls", e);
            PaginatedResponse<VoiceCallResponse> errorResponse = new PaginatedResponse<>(null, null);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Get a specific voice call by ID.
     * 
     * GET /api/voice-calls/{callId}
     *
     * @param callId the ID of the call
     * @return the voice call details
     */
    @GetMapping("/voice-calls/{callId}")
    public ResponseEntity<ApiResponse<VoiceCallResponse>> getVoiceCall(@PathVariable UUID callId) {
        logger.info("Fetching voice call: {}", callId);

        try {
            VoiceCallResponse response = voiceCallService.getCall(callId);

            return ResponseEntity.ok(
                ApiResponse.success(response, "Voice call retrieved successfully")
            );

        } catch (IllegalArgumentException e) {
            logger.error("Voice call not found: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to fetch voice call", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch voice call: " + e.getMessage()));
        }
    }
}
