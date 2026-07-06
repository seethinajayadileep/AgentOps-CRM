package com.agentopscrm.controller;

import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.dto.BulkImportDiscoveredLeadsRequest;
import com.agentopscrm.dto.BulkImportResultResponse;
import com.agentopscrm.dto.DiscoveredLeadResponse;
import com.agentopscrm.dto.ImportDiscoveredLeadRequest;
import com.agentopscrm.dto.LeadSourceRunResponse;
import com.agentopscrm.dto.StartLeadFinderRunRequest;
import com.agentopscrm.service.LeadFinderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Apify Lead Finder (F-010).
 *
 * Why exists: Exposes outbound lead discovery operations (start run, list/get runs, sync
 * results, import/reject discovered leads). Never exposes the Apify token to the client.
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@RestController
@RequestMapping("/api/lead-finder")
@CrossOrigin(origins = "*")
public class LeadFinderController {

    private static final Logger logger = LoggerFactory.getLogger(LeadFinderController.class);

    private final LeadFinderService leadFinderService;

    public LeadFinderController(LeadFinderService leadFinderService) {
        this.leadFinderService = leadFinderService;
    }

    /**
     * GET /api/lead-finder/config - report whether Apify is configured (no secrets exposed).
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<ConfigStatus>> getConfig() {
        ConfigStatus status = new ConfigStatus(leadFinderService.isApifyConfigured());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * POST /api/lead-finder/runs - start a new lead discovery run.
     */
    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<LeadSourceRunResponse>> startRun(
        @Valid @RequestBody StartLeadFinderRunRequest request) {
        logger.info("POST /api/lead-finder/runs - searchName: {}", request.getSearchName());
        try {
            LeadSourceRunResponse response = leadFinderService.startRun(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lead discovery run started"));
        } catch (LeadFinderService.ApifyNotConfiguredException e) {
            logger.warn("Lead finder run rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to start lead discovery run", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to start lead discovery run: " + e.getMessage()));
        }
    }

    /**
     * GET /api/lead-finder/runs - list all runs.
     */
    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<List<LeadSourceRunResponse>>> listRuns() {
        try {
            return ResponseEntity.ok(ApiResponse.success(leadFinderService.listRuns()));
        } catch (Exception e) {
            logger.error("Failed to list lead finder runs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to list runs: " + e.getMessage()));
        }
    }

    /**
     * GET /api/lead-finder/runs/{id} - get one run.
     */
    @GetMapping("/runs/{id}")
    public ResponseEntity<ApiResponse<LeadSourceRunResponse>> getRun(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(leadFinderService.getRun(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to get run {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get run: " + e.getMessage()));
        }
    }

    /**
     * GET /api/lead-finder/runs/{id}/results - discovered leads for a run.
     */
    @GetMapping("/runs/{id}/results")
    public ResponseEntity<ApiResponse<List<DiscoveredLeadResponse>>> getResults(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(leadFinderService.getResults(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to get results for run {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get results: " + e.getMessage()));
        }
    }

    /**
     * POST /api/lead-finder/runs/{id}/sync - fetch/sync results from Apify.
     */
    @PostMapping("/runs/{id}/sync")
    public ResponseEntity<ApiResponse<LeadSourceRunResponse>> syncRun(@PathVariable UUID id) {
        logger.info("POST /api/lead-finder/runs/{}/sync", id);
        try {
            LeadSourceRunResponse response = leadFinderService.syncRun(id);
            return ResponseEntity.ok(ApiResponse.success(response, "Run synced"));
        } catch (LeadFinderService.ApifyNotConfiguredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to sync run {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to sync run: " + e.getMessage()));
        }
    }

    /**
     * POST /api/lead-finder/discovered-leads/{id}/import - import one discovered lead.
     */
    @PostMapping("/discovered-leads/{id}/import")
    public ResponseEntity<ApiResponse<DiscoveredLeadResponse>> importLead(
        @PathVariable UUID id,
        @RequestBody(required = false) ImportDiscoveredLeadRequest request) {
        logger.info("POST /api/lead-finder/discovered-leads/{}/import", id);
        UUID targetBusinessId = request != null ? request.getTargetBusinessId() : null;
        try {
            DiscoveredLeadResponse response = leadFinderService.importDiscoveredLead(id, targetBusinessId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Discovered lead imported"));
        } catch (LeadFinderService.DuplicateLeadException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to import discovered lead {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to import discovered lead: " + e.getMessage()));
        }
    }

    /**
     * POST /api/lead-finder/discovered-leads/import-bulk - import selected discovered leads.
     */
    @PostMapping("/discovered-leads/import-bulk")
    public ResponseEntity<ApiResponse<BulkImportResultResponse>> importBulk(
        @Valid @RequestBody BulkImportDiscoveredLeadsRequest request) {
        logger.info("POST /api/lead-finder/discovered-leads/import-bulk - count: {}",
            request.getDiscoveredLeadIds() != null ? request.getDiscoveredLeadIds().size() : 0);
        try {
            BulkImportResultResponse result = leadFinderService.importBulk(
                request.getDiscoveredLeadIds(), request.getTargetBusinessId());
            return ResponseEntity.ok(ApiResponse.success(result,
                "Imported " + result.getImported() + " of " + result.getRequested() + " leads"));
        } catch (Exception e) {
            logger.error("Failed bulk import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed bulk import: " + e.getMessage()));
        }
    }

    /**
     * POST /api/lead-finder/discovered-leads/{id}/reject - reject a discovered lead.
     */
    @PostMapping("/discovered-leads/{id}/reject")
    public ResponseEntity<ApiResponse<DiscoveredLeadResponse>> rejectLead(@PathVariable UUID id) {
        logger.info("POST /api/lead-finder/discovered-leads/{}/reject", id);
        try {
            DiscoveredLeadResponse response = leadFinderService.rejectDiscoveredLead(id);
            return ResponseEntity.ok(ApiResponse.success(response, "Discovered lead rejected"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to reject discovered lead {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to reject discovered lead: " + e.getMessage()));
        }
    }

    /**
     * Small payload reporting Apify configuration status to the frontend (no secrets).
     */
    public static class ConfigStatus {
        private final boolean apifyConfigured;

        public ConfigStatus(boolean apifyConfigured) {
            this.apifyConfigured = apifyConfigured;
        }

        public boolean isApifyConfigured() {
            return apifyConfigured;
        }
    }
}
