package com.agentopscrm.controller;

import com.agentopscrm.dto.settings.*;
import com.agentopscrm.entity.enums.ReadinessStatus;
import com.agentopscrm.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for system settings, diagnostics, and integration readiness.
 * 
 * Security Note: These endpoints expose operational information and should be 
 * administrator-only when authentication is implemented.
 * 
 * @author AgentOps Team
 * @version 0.1.0
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * GET /api/settings/overview
     * 
     * Returns system health overview including all integration statuses.
     */
    @GetMapping("/overview")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        log.info("GET /api/settings/overview - Fetching system health overview");
        SystemHealthResponse response = settingsService.getSystemHealth();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/settings/integrations
     * 
     * Returns detailed integration readiness for all providers.
     */
    @GetMapping("/integrations")
    public ResponseEntity<IntegrationsResponse> getIntegrations() {
        log.info("GET /api/settings/integrations - Fetching integrations status");
        IntegrationsResponse response = settingsService.getIntegrations();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/settings/models
     * 
     * Returns effective AI model configuration.
     */
    @GetMapping("/models")
    public ResponseEntity<ModelsConfigResponse> getModelsConfig() {
        log.info("GET /api/settings/models - Fetching AI models configuration");
        ModelsConfigResponse response = settingsService.getModelsConfig();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/settings/rag
     * 
     * Returns RAG/Knowledge Base configuration and metrics.
     */
    @GetMapping("/rag")
    public ResponseEntity<RagConfigResponse> getRagConfig() {
        log.info("GET /api/settings/rag - Fetching RAG configuration");
        RagConfigResponse response = settingsService.getRagConfig();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/settings/voice
     * 
     * Returns Voice AI (Vapi) configuration and metrics.
     */
    @GetMapping("/voice")
    public ResponseEntity<VoiceConfigResponse> getVoiceConfig() {
        log.info("GET /api/settings/voice - Fetching Voice AI configuration");
        try {
            VoiceConfigResponse response = settingsService.getVoiceConfig();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Never surface a bare 500 for the Voice Settings panel - a missing or
            // misconfigured Vapi integration must degrade gracefully to a safe
            // NOT_CONFIGURED readiness response instead of failing the request.
            log.error("Failed to build voice configuration response; returning safe NOT_CONFIGURED fallback", e);
            VoiceConfigResponse fallback = new VoiceConfigResponse();
            fallback.setEnabled(false);
            fallback.setApiKeyConfigured(false);
            fallback.setAssistantIdConfigured(false);
            fallback.setPhoneNumberIdConfigured(false);
            fallback.setWebhookSecretConfigured(false);
            fallback.setWebhookEndpoint("/api/vapi/webhook");
            fallback.setStatus(ReadinessStatus.NOT_CONFIGURED);
            fallback.setStatusMessage("Vapi configuration is incomplete");
            fallback.setTotalCalls(0L);
            fallback.setSuccessfulCalls(0L);
            fallback.setFailedCalls(0L);
            return ResponseEntity.ok(fallback);
        }
    }

    /**
     * GET /api/settings/agents
     * 
 * Returns agent readiness and safety configuration.
     */
    @GetMapping("/agents")
    public ResponseEntity<AgentsResponse> getAgentsConfig() {
        log.info("GET /api/settings/agents - Fetching agents configuration");
        AgentsResponse response = settingsService.getAgentsConfig();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/settings/system
     * 
     * Returns safe system diagnostics and configuration.
     */
    @GetMapping("/system")
    public ResponseEntity<SystemDiagnosticsResponse> getSystemDiagnostics() {
        log.info("GET /api/settings/system - Fetching system diagnostics");
        SystemDiagnosticsResponse response = settingsService.getSystemDiagnostics();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/settings/integrations/{integration}/test
     * 
     * Test a specific integration connection.
     * 
     * @param integration Integration name (e.g., "database", "openai", "firecrawl", "apify", "vapi", "redis")
     */
    @PostMapping("/integrations/{integration}/test")
    public ResponseEntity<IntegrationTestResult> testIntegration(@PathVariable String integration) {
        log.info("POST /api/settings/integrations/{}/test - Testing integration", integration);
        IntegrationTestResult result = settingsService.testIntegration(integration);
        return ResponseEntity.ok(result);
    }
}
