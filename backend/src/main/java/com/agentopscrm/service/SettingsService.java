package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.client.FirecrawlClient;
import com.agentopscrm.client.VapiClient;
import com.agentopscrm.dto.settings.*;
import com.agentopscrm.entity.VoiceCall;
import com.agentopscrm.util.AppVersion;
import com.agentopscrm.util.SafeErrorMessages;
import com.agentopscrm.entity.enums.ReadinessStatus;
import com.agentopscrm.entity.enums.VoiceCallStatus;
import com.agentopscrm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for handling settings, diagnostics, and integration readiness checks.
 * 
 * Security Note: Never returns secret values. All responses are safe for frontend consumption.
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final DataSource dataSource;
    private final HealthEndpoint healthEndpoint;
    private final FirecrawlClient firecrawlClient;
    private final VapiClient vapiClient;
    private final ApifyClient apifyClient;
    private final EmbeddingService embeddingService;
    private final BusinessRepository businessRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final VoiceCallRepository voiceCallRepository;
    private final AgentLogRepository agentLogRepository;
    
    @Value("${spring.application.name:agentops-crm}")
    private String applicationName;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @Value("${rag.vector-store:postgres-text}")
    private String vectorStoreStrategy;
    
    @Value("${rag.embedding.model:text-embedding-3-small}")
    private String embeddingModel;
    
    @Value("${rag.embedding.dimension:1536}")
    private Integer embeddingDimension;
    
    @Value("${rag.search.default-top-k:5}")
    private Integer defaultTopK;
    
    @Value("${rag.search.max-top-k:50}")
    private Integer maxTopK;
    
    @Value("${spring.flyway.enabled:false}")
    private boolean flywayEnabled;
    
    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String hibernateDdlAuto;
    
    @Value("${vapi.enabled:false}")
    private boolean vapiEnabled;
    
    @Value("${vapi.api-key:}")
    private String vapiApiKey;
    
    @Value("${vapi.assistant-id:}")
    private String vapiAssistantId;
    
    @Value("${vapi.phone-number-id:}")
    private String vapiPhoneNumberId;
    
    @Value("${vapi.webhook-secret:}")
    private String vapiWebhookSecret;
    
    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;
    
    @Value("${apify.enabled:false}")
    private boolean apifyEnabled;

    public SettingsService(
            DataSource dataSource,
            HealthEndpoint healthEndpoint,
            FirecrawlClient firecrawlClient,
            VapiClient vapiClient,
            ApifyClient apifyClient,
            EmbeddingService embeddingService,
            BusinessRepository businessRepository,
            DocumentRepository documentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            VoiceCallRepository voiceCallRepository,
            AgentLogRepository agentLogRepository) {
        this.dataSource = dataSource;
        this.healthEndpoint = healthEndpoint;
        this.firecrawlClient = firecrawlClient;
        this.vapiClient = vapiClient;
        this.apifyClient = apifyClient;
        this.embeddingService = embeddingService;
        this.businessRepository = businessRepository;
        this.documentRepository = documentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.voiceCallRepository = voiceCallRepository;
        this.agentLogRepository = agentLogRepository;
    }

    /**
     * Get system health overview.
     */
    public SystemHealthResponse getSystemHealth() {
        SystemHealthResponse response = new SystemHealthResponse();
        response.setApplicationName(applicationName);
        response.setApplicationVersion(AppVersion.VALUE);
        response.setActiveProfile(activeProfile);
        response.setEnvironment(activeProfile);
        response.setServerTime(Instant.now());
        response.setLastHealthCheck(Instant.now());

        Map<String, ReadinessStatus> components = new LinkedHashMap<>();
        components.put("backend", ReadinessStatus.HEALTHY);
        components.put("database", checkDatabaseStatus());
        components.put("redis", checkRedisStatus());
        components.put("openai", checkOpenAIStatus());
        components.put("firecrawl", checkFirecrawlStatus());
        components.put("apify", checkApifyStatus());
        components.put("vapi", checkVapiStatus());

        response.setComponents(components);
        return response;
    }

    /**
     * Get integrations overview.
     */
    public IntegrationsResponse getIntegrations() {
        List<IntegrationStatus> integrations = new ArrayList<>();

        // OpenAI
        IntegrationStatus openai = new IntegrationStatus();
        openai.setName("OpenAI");
        openai.setPurpose("RAG answers, embeddings, evaluation, lead qualification, follow-up generation");
        openai.setConfigured(embeddingService.isConfigured());
        openai.setEnabled(embeddingService.isConfigured());
        openai.setStatus(checkOpenAIStatus());
        openai.setMessage(embeddingService.isConfigured() ? "OpenAI API key configured" : "API key not configured");
        openai.setConfigDetails("Managed through environment configuration");
        openai.setLastChecked(Instant.now());
        integrations.add(openai);

        // Firecrawl
        IntegrationStatus firecrawl = new IntegrationStatus();
        firecrawl.setName("Firecrawl");
        firecrawl.setPurpose("Website crawling and research");
        firecrawl.setConfigured(firecrawlClient.isConfigured());
        firecrawl.setEnabled(firecrawlClient.isConfigured());
        firecrawl.setStatus(checkFirecrawlStatus());
        firecrawl.setMessage(firecrawlClient.isConfigured() ? "Firecrawl API key configured" : "API key not configured");
        firecrawl.setConfigDetails("Managed through environment configuration");
        firecrawl.setLastChecked(Instant.now());
        integrations.add(firecrawl);

        // Apify
        IntegrationStatus apify = new IntegrationStatus();
        apify.setName("Apify");
        apify.setPurpose("Lead discovery");
        apify.setConfigured(apifyClient.isConfigured());
        apify.setEnabled(apifyEnabled);
        apify.setStatus(checkApifyStatus());
        apify.setMessage(getApifyMessage());
        apify.setConfigDetails("Managed through environment configuration");
        apify.setLastChecked(Instant.now());
        integrations.add(apify);

        // Vapi
        IntegrationStatus vapi = new IntegrationStatus();
        vapi.setName("Vapi");
        vapi.setPurpose("AI voice calls");
        vapi.setConfigured(isVapiConfigured());
        vapi.setEnabled(isVapiOperational());
        vapi.setStatus(checkVapiStatus());
        vapi.setMessage(getVapiMessage());
        vapi.setConfigDetails("Managed through environment configuration");
        vapi.setLastChecked(Instant.now());
        integrations.add(vapi);

        // PostgreSQL
        IntegrationStatus postgres = new IntegrationStatus();
        postgres.setName("PostgreSQL");
        postgres.setPurpose("CRM data and vector storage");
        postgres.setConfigured(true);
        postgres.setEnabled(true);
        postgres.setStatus(checkDatabaseStatus());
        postgres.setMessage("Database connection active");
        postgres.setConfigDetails("Managed through environment configuration");
        postgres.setLastChecked(Instant.now());
        integrations.add(postgres);

        // Redis is optional and unused by the CRM runtime — do not claim HEALTHY.
        IntegrationStatus redis = new IntegrationStatus();
        redis.setName("Redis");
        redis.setPurpose("Optional cache (not used by current CRM features)");
        redis.setConfigured(false);
        redis.setEnabled(false);
        redis.setStatus(ReadinessStatus.UNKNOWN);
        redis.setMessage("Redis is not used by this application. Connection testing is not available.");
        redis.setConfigDetails("No Redis client is wired into the CRM");
        redis.setLastChecked(Instant.now());
        integrations.add(redis);

        return new IntegrationsResponse(integrations);
    }

    /**
     * Get AI models configuration.
     */
    public ModelsConfigResponse getModelsConfig() {
        ModelsConfigResponse response = new ModelsConfigResponse();
        response.setRagAnswerModel("gpt-4o-mini");
        response.setEvaluationModel("gpt-4o-mini");
        response.setLeadQualificationModel("gpt-4o-mini");
        response.setFollowUpModel("gpt-4o-mini");
        response.setEmbeddingProvider("openai");
        response.setEmbeddingModel(embeddingModel);
        response.setEmbeddingDimension(embeddingDimension);
        response.setConfigNote("Managed through application/environment configuration");
        return response;
    }

    /**
     * Get RAG/Knowledge Base configuration and metrics.
     */
    public RagConfigResponse getRagConfig() {
        RagConfigResponse response = new RagConfigResponse();
        response.setEmbeddingProvider("openai");
        response.setEmbeddingModel(embeddingModel);
        response.setEmbeddingDimension(embeddingDimension);
        response.setVectorStoreStrategy(vectorStoreStrategy);
        response.setDefaultTopK(defaultTopK);
        response.setMaxTopK(maxTopK);

        // Metrics
        response.setTotalBusinesses(businessRepository.count());
        response.setBusinessesWithDocuments(countBusinessesWithDocuments());
        response.setBusinessesWithKnowledge(countBusinessesWithKnowledge());
        response.setTotalDocuments(documentRepository.count());
        response.setTotalKnowledgeChunks(knowledgeChunkRepository.count());

        // Warning for postgres-text vector storage
        if ("postgres-text".equals(vectorStoreStrategy)) {
            response.setVectorStoreWarning("Embeddings are currently stored as text and ranked in memory. pgvector is the planned production strategy.");
        }

        return response;
    }

    /**
     * Get Voice AI (Vapi) configuration.
     */
    public VoiceConfigResponse getVoiceConfig() {
        VoiceConfigResponse response = new VoiceConfigResponse();
        
        boolean apiKeyConfigured = isNonBlank(vapiApiKey);
        boolean assistantIdConfigured = isNonBlank(vapiAssistantId);
        boolean phoneNumberIdConfigured = isNonBlank(vapiPhoneNumberId);
        boolean webhookSecretConfigured = isNonBlank(vapiWebhookSecret);
        
        response.setEnabled(isVapiOperational());
        response.setApiKeyConfigured(apiKeyConfigured);
        response.setAssistantIdConfigured(assistantIdConfigured);
        response.setPhoneNumberIdConfigured(phoneNumberIdConfigured);
        response.setWebhookSecretConfigured(webhookSecretConfigured);
        response.setWebhookEndpoint("/api/webhooks/vapi");
        // Provide complete webhook URL from backend
        response.setWebhookUrl(publicBaseUrl + "/api/webhooks/vapi");
        
        // Determine status and message based on configuration
        ReadinessStatus status;
        String message;
        
        if (isVapiConfigured()) {
            status = ReadinessStatus.CONFIGURED;
            message = vapiEnabled
                    ? "Vapi configuration is present."
                    : "Vapi credentials are present. Calls can be received; set VAPI_ENABLED=true for CRM-started outbound calls.";
        } else if (!vapiEnabled) {
            status = ReadinessStatus.DISABLED;
            message = "Voice calling is disabled and no Vapi credentials are configured.";
        } else if (!apiKeyConfigured || !assistantIdConfigured || !phoneNumberIdConfigured) {
            status = ReadinessStatus.NOT_CONFIGURED;
            
            // Build a helpful message about what's missing
            List<String> missing = new ArrayList<>();
            if (!apiKeyConfigured) missing.add("API key");
            if (!assistantIdConfigured) missing.add("Assistant ID");
            if (!phoneNumberIdConfigured) missing.add("Phone Number ID");
            
            message = "Vapi configuration is incomplete. Missing: " + String.join(", ", missing) + ".";
        } else {
            status = ReadinessStatus.CONFIGURED;
            message = "Vapi configuration is present.";
        }
        
        response.setStatus(status);
        response.setStatusMessage(message);

        // Voice call metrics - keep readiness and metrics status separate
        // A metrics query failure should NOT change the Vapi readiness status
        try {
            response.setTotalCalls(voiceCallRepository.count());
            response.setSuccessfulCalls(voiceCallRepository.countByStatus(VoiceCallStatus.COMPLETED));
            response.setFailedCalls(voiceCallRepository.countByStatus(VoiceCallStatus.FAILED));

            // Use sorted query to reliably get the latest calls
            List<VoiceCall> completedCalls = voiceCallRepository.findByStatusOrderByCreatedAtDesc(
                VoiceCallStatus.COMPLETED, PageRequest.of(0, 1)).getContent();
            if (!completedCalls.isEmpty()) {
                response.setLastSuccessfulCall(completedCalls.get(0).getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
            }

            List<VoiceCall> failedCalls = voiceCallRepository.findByStatusOrderByCreatedAtDesc(
                VoiceCallStatus.FAILED, PageRequest.of(0, 1)).getContent();
            if (!failedCalls.isEmpty()) {
                response.setLastFailedCall(failedCalls.get(0).getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
            }
            
            // Metrics loaded successfully
            response.setMetricsAvailable(true);
            response.setMetricsMessage("Voice call metrics are available.");
        } catch (Exception e) {
            log.error("Failed to load voice call metrics; returning configuration status without metrics", e);
            response.setTotalCalls(0L);
            response.setSuccessfulCalls(0L);
            response.setFailedCalls(0L);
            // Keep Vapi readiness status unchanged - only mark metrics as unavailable
            response.setMetricsAvailable(false);
            response.setMetricsMessage("Voice call metrics are temporarily unavailable.");
        }

        return response;
    }

    /**
     * Get agents readiness and safety configuration.
     */
    public AgentsResponse getAgentsConfig() {
        List<AgentStatus> agents = new ArrayList<>();

        boolean openaiReady = embeddingService.isConfigured();

        AgentStatus support = new AgentStatus(
                "Support Agent",
                openaiReady ? ReadinessStatus.HEALTHY : ReadinessStatus.NOT_CONFIGURED,
                "Answers customer questions from the business knowledge base");
        support.setRequiredIntegration("OpenAI");
        support.setCurrentModel("gpt-4o-mini");
        support.setFallbackAvailable(false);
        agents.add(support);

        AgentStatus evaluation = new AgentStatus(
                "Evaluation Agent",
                openaiReady ? ReadinessStatus.HEALTHY : ReadinessStatus.DEGRADED,
                "Checks support answers for safety and quality");
        evaluation.setRequiredIntegration("OpenAI");
        evaluation.setCurrentModel("gpt-4o-mini");
        evaluation.setFallbackAvailable(true);
        agents.add(evaluation);

        AgentStatus leadQual = new AgentStatus(
                "Lead Qualification Agent",
                openaiReady ? ReadinessStatus.HEALTHY : ReadinessStatus.NOT_CONFIGURED,
                "Scores inbound conversations and creates CRM leads");
        leadQual.setRequiredIntegration("OpenAI");
        leadQual.setCurrentModel("gpt-4o-mini");
        leadQual.setFallbackAvailable(false);
        agents.add(leadQual);

        AgentStatus followUp = new AgentStatus(
                "Follow-up Agent",
                openaiReady ? ReadinessStatus.HEALTHY : ReadinessStatus.NOT_CONFIGURED,
                "Drafts outbound follow-up messages for human approval");
        followUp.setRequiredIntegration("OpenAI");
        followUp.setCurrentModel("gpt-4o-mini");
        followUp.setFallbackAvailable(false);
        agents.add(followUp);

        // Website Research/Crawler
        AgentStatus crawler = new AgentStatus(
                "Website Research/Crawler",
                checkFirecrawlStatus(),
                "Crawls business websites to collect knowledge-base source pages");
        crawler.setRequiredIntegration("Firecrawl");
        agents.add(crawler);

        // Knowledge Base Builder
        AgentStatus kb = new AgentStatus(
                "Knowledge Base Builder",
                embeddingService.isConfigured() ? ReadinessStatus.HEALTHY : ReadinessStatus.NOT_CONFIGURED,
                "Chunks crawled pages and stores embeddings for retrieval");
        kb.setRequiredIntegration("OpenAI");
        agents.add(kb);

        // Lead Finder Agent
        AgentStatus leadFinder = new AgentStatus(
                "Lead Finder Agent",
                checkApifyStatus(),
                "Discovers outbound prospects through the Apify integration");
        leadFinder.setRequiredIntegration("Apify");
        agents.add(leadFinder);

        // Voice Agent
        AgentStatus voice = new AgentStatus(
                "Voice Agent",
                checkVapiStatus(),
                "Places and transcribes AI voice calls through Vapi");
        voice.setRequiredIntegration("Vapi");
        agents.add(voice);

        // Safety Config
        AgentsResponse.SafetyConfig safety = new AgentsResponse.SafetyConfig();
        safety.setEvaluationEnabled(true);
        safety.setUnsafeAnswerBlocking(true);
        safety.setFallbackAnswerAvailable(true);
        safety.setHumanApprovalEnabled(true); // Follow-ups
        safety.setHumanApprovalForVoice(false); // Auto-approved voice calls
        safety.setLeadCaptureBehavior("Automatic capture on email/phone");

        return new AgentsResponse(agents, safety);
    }

    /**
     * Get system diagnostics.
     */
    public SystemDiagnosticsResponse getSystemDiagnostics() {
        SystemDiagnosticsResponse response = new SystemDiagnosticsResponse();
        response.setApplicationName(applicationName);
        response.setApplicationVersion(AppVersion.VALUE);
        response.setBackendVersion(AppVersion.VALUE);
        response.setActiveProfile(activeProfile);
        response.setApiBasePath("/api");
        response.setServerTimezone(ZoneId.systemDefault().getId());
        response.setDatabaseType("PostgreSQL");
        response.setRedisConfigured(false);
        response.setFlywayEnabled(flywayEnabled);
        response.setHibernateSchemaMode(hibernateDdlAuto);
        response.setVectorStoreStrategy(vectorStoreStrategy);

        // Warnings
        List<SystemDiagnosticsResponse.SystemWarning> warnings = new ArrayList<>();
        if (!flywayEnabled) {
            warnings.add(new SystemDiagnosticsResponse.SystemWarning(
                    "MIGRATION",
                    "Flyway migrations are disabled",
                    "Production should use validated Flyway migrations"
            ));
        }
        if ("update".equals(hibernateDdlAuto)) {
            warnings.add(new SystemDiagnosticsResponse.SystemWarning(
                    "SCHEMA",
                    "Hibernate ddl-auto=update is enabled",
                    "Production should use Flyway migrations with ddl-auto=validate"
            ));
        }
        if ("postgres-text".equals(vectorStoreStrategy)) {
            warnings.add(new SystemDiagnosticsResponse.SystemWarning(
                    "VECTOR_STORE",
                    "Using postgres-text vector storage (in-memory ranking)",
                    "Consider upgrading to pgvector for production"
            ));
        }

        response.setWarnings(warnings);
        return response;
    }

    /**
     * Test an integration connection (safe, non-destructive).
     */
    public IntegrationTestResult testIntegration(String integrationName) {
        long startTime = System.currentTimeMillis();
        IntegrationTestResult result = new IntegrationTestResult();
        result.setIntegration(integrationName);
        result.setTestedAt(Instant.now());

        try {
            switch (integrationName.toLowerCase()) {
                case "database":
                case "postgresql":
                    testDatabase();
                    result.setSuccess(true);
                    result.setStatus(ReadinessStatus.HEALTHY);
                    result.setMessage("Database connection successful");
                    result.setCheckType("LIVE");
                    break;

                case "redis":
                    result.setSuccess(false);
                    result.setStatus(ReadinessStatus.UNKNOWN);
                    result.setMessage("Redis is not used by this application. Status remains UNKNOWN / NOT USED.");
                    result.setCheckType("NOT_USED");
                    break;

                case "openai":
                    if (!embeddingService.isConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(ReadinessStatus.NOT_CONFIGURED);
                        result.setMessage("OpenAI API key not configured");
                        result.setCheckType("LIVE");
                    } else {
                        try {
                            embeddingService.ping();
                            result.setSuccess(true);
                            result.setStatus(ReadinessStatus.CONNECTED);
                            result.setMessage("CONNECTED — authenticated with OpenAI");
                            result.setCheckType("LIVE");
                        } catch (Exception e) {
                            applyProviderFailure(result, e);
                            result.setCheckType("LIVE");
                        }
                    }
                    break;

                case "firecrawl":
                    if (!firecrawlClient.isConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(ReadinessStatus.NOT_CONFIGURED);
                        result.setMessage("Firecrawl API key not configured");
                        result.setCheckType("LIVE");
                    } else {
                        try {
                            firecrawlClient.ping();
                            result.setSuccess(true);
                            result.setStatus(ReadinessStatus.CONNECTED);
                            result.setMessage("CONNECTED — authenticated with Firecrawl");
                            result.setCheckType("LIVE");
                        } catch (Exception e) {
                            applyProviderFailure(result, e);
                            result.setCheckType("LIVE");
                        }
                    }
                    break;

                case "apify":
                    if (!apifyClient.isConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(apifyEnabled ? ReadinessStatus.NOT_CONFIGURED : ReadinessStatus.DISABLED);
                        result.setMessage(getApifyMessage());
                    } else {
                        try {
                            apifyClient.ping();
                            result.setSuccess(true);
                            result.setStatus(ReadinessStatus.HEALTHY);
                            result.setMessage("CONNECTED — authenticated with Apify");
                            result.setCheckType("LIVE");
                        } catch (ApifyClient.ApifyException e) {
                            if (e.unauthorized) {
                                result.setSuccess(false);
                                result.setStatus(ReadinessStatus.ERROR);
                                result.setMessage("FAILED — Apify rejected the stored credentials");
                            } else if (SafeErrorMessages.isTlsOrConnectivity(e)) {
                                result.setSuccess(false);
                                result.setStatus(ReadinessStatus.DEGRADED);
                                result.setMessage("DEGRADED — could not complete a TLS connection to Apify");
                            } else {
                                result.setSuccess(false);
                                result.setStatus(ReadinessStatus.ERROR);
                                result.setMessage("FAILED — " + SafeErrorMessages.classify(e));
                            }
                        }
                    }
                    break;

                case "vapi":
                    if (!isVapiConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(vapiEnabled ? ReadinessStatus.NOT_CONFIGURED : ReadinessStatus.DISABLED);
                        result.setMessage(getVapiMessage());
                        result.setCheckType("LIVE");
                    } else {
                        try {
                            vapiClient.pingAssistant(vapiAssistantId);
                            result.setSuccess(true);
                            result.setStatus(ReadinessStatus.CONNECTED);
                            result.setMessage("CONNECTED — authenticated with Vapi (assistant lookup only; no call placed)");
                            result.setCheckType("LIVE");
                        } catch (Exception e) {
                            applyProviderFailure(result, e);
                            result.setCheckType("LIVE");
                        }
                    }
                    break;

                default:
                    result.setSuccess(false);
                    result.setStatus(ReadinessStatus.UNKNOWN);
                    result.setMessage("Unknown integration: " + integrationName);
            }
        } catch (Exception e) {
            log.error("Integration test failed for {}", integrationName, e);
            result.setSuccess(false);
            result.setStatus(ReadinessStatus.ERROR);
            result.setMessage("Test failed: " + SafeErrorMessages.classify(e));
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    // ===== Private Helper Methods =====

    private ReadinessStatus checkDatabaseStatus() {
        try {
            testDatabase();
            return ReadinessStatus.HEALTHY;
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return ReadinessStatus.ERROR;
        }
    }

    private void applyProviderFailure(IntegrationTestResult result, Exception e) {
        if (SafeErrorMessages.isTlsOrConnectivity(e)) {
            result.setSuccess(false);
            result.setStatus(ReadinessStatus.DEGRADED);
            result.setMessage("DEGRADED — could not complete a TLS connection to the provider");
        } else if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("credential")) {
            result.setSuccess(false);
            result.setStatus(ReadinessStatus.ERROR);
            result.setMessage("FAILED — " + SafeErrorMessages.CREDENTIALS);
        } else {
            result.setSuccess(false);
            result.setStatus(ReadinessStatus.ERROR);
            result.setMessage("FAILED — " + SafeErrorMessages.classify(e));
        }
    }

    private void testDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(2)) {
                throw new Exception("Database connection is not valid");
            }
        }
    }

    private ReadinessStatus checkRedisStatus() {
        return ReadinessStatus.UNKNOWN;
    }

    private ReadinessStatus checkOpenAIStatus() {
        return embeddingService.isConfigured() ? ReadinessStatus.CONFIGURED : ReadinessStatus.NOT_CONFIGURED;
    }

    private ReadinessStatus checkFirecrawlStatus() {
        return firecrawlClient.isConfigured() ? ReadinessStatus.CONFIGURED : ReadinessStatus.NOT_CONFIGURED;
    }

    private ReadinessStatus checkApifyStatus() {
        if (!apifyEnabled) {
            return ReadinessStatus.DISABLED;
        }
        return apifyClient.isConfigured() ? ReadinessStatus.CONFIGURED : ReadinessStatus.NOT_CONFIGURED;
    }

    private ReadinessStatus checkVapiStatus() {
        if (isVapiConfigured()) {
            return ReadinessStatus.CONFIGURED;
        }
        return vapiEnabled ? ReadinessStatus.NOT_CONFIGURED : ReadinessStatus.DISABLED;
    }

    private boolean isVapiConfigured() {
        return isNonBlank(vapiApiKey) && isNonBlank(vapiAssistantId) && isNonBlank(vapiPhoneNumberId);
    }

    private boolean isVapiOperational() {
        return isVapiConfigured();
    }

    private String getVapiMessage() {
        if (isVapiConfigured()) {
            return vapiEnabled
                    ? "Vapi configured and enabled"
                    : "Vapi credentials are present. Inbound and existing calls work; enable VAPI_ENABLED for CRM-started outbound calls.";
        }
        if (!vapiEnabled) {
            return "Vapi disabled";
        }
        return "Vapi enabled but configuration incomplete";
    }

    private String getApifyMessage() {
        if (!apifyEnabled) {
            return "Apify disabled";
        }
        if (!apifyClient.isConfigured()) {
            return "Apify enabled but API token not configured";
        }
        return "Apify configured and enabled";
    }

    private long countBusinessesWithDocuments() {
        try {
            return businessRepository.findAll().stream()
                    .filter(b -> documentRepository.findByBusinessId(b.getId()).size() > 0)
                    .count();
        } catch (Exception e) {
            log.error("Failed to count businesses with documents", e);
            return 0;
        }
    }

    private long countBusinessesWithKnowledge() {
        try {
            return businessRepository.findAll().stream()
                    .filter(b -> knowledgeChunkRepository.countByBusinessId(b.getId()) > 0)
                    .count();
        } catch (Exception e) {
            log.error("Failed to count businesses with knowledge", e);
            return 0;
        }
    }

    private boolean isNonBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        // Treat placeholder values as unconfigured
        String trimmed = value.trim();
        return !trimmed.startsWith("your_") && !trimmed.startsWith("sk-...") && 
               !trimmed.contains("_here") && !trimmed.equals("...") &&
               !trimmed.equals("change-in-production");
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        // Remove any potential secrets from error messages
        return message.replaceAll("(?i)(api[_-]?key|token|secret|password)[=:\\s]+[a-zA-Z0-9_-]+", "$1=***");
    }
}
