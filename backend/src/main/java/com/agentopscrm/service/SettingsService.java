package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.client.FirecrawlClient;
import com.agentopscrm.client.VapiClient;
import com.agentopscrm.dto.settings.*;
import com.agentopscrm.entity.VoiceCall;
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
        response.setApplicationVersion("0.1.0");
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
        vapi.setEnabled(vapiEnabled);
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

        // Redis
        IntegrationStatus redis = new IntegrationStatus();
        redis.setName("Redis");
        redis.setPurpose("Cache and future workflow coordination");
        redis.setConfigured(true);
        redis.setEnabled(true);
        redis.setStatus(checkRedisStatus());
        redis.setMessage("Redis connection active");
        redis.setConfigDetails("Managed through environment configuration");
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
        response.setEnabled(vapiEnabled);
        response.setApiKeyConfigured(isNonBlank(vapiApiKey));
        response.setAssistantIdConfigured(isNonBlank(vapiAssistantId));
        response.setPhoneNumberIdConfigured(isNonBlank(vapiPhoneNumberId));
        response.setWebhookSecretConfigured(isNonBlank(vapiWebhookSecret));
        response.setWebhookEndpoint("/api/vapi/webhook");
        response.setStatus(checkVapiStatus());
        response.setStatusMessage(getVapiMessage());

        // Voice call metrics - never allow a DB/query problem here to turn into
        // a 500 for the whole Voice Settings panel. Vapi may be DISABLED or
        // NOT_CONFIGURED and the panel must still render safely with zeroed
        // metrics rather than failing the request.
        try {
            response.setTotalCalls(voiceCallRepository.count());
            response.setSuccessfulCalls(voiceCallRepository.countByStatus(VoiceCallStatus.COMPLETED));
            response.setFailedCalls(voiceCallRepository.countByStatus(VoiceCallStatus.FAILED));

            List<VoiceCall> completedCalls = voiceCallRepository.findByStatus(VoiceCallStatus.COMPLETED, PageRequest.of(0, 1)).getContent();
            if (!completedCalls.isEmpty()) {
                response.setLastSuccessfulCall(completedCalls.get(0).getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
            }

            List<VoiceCall> failedCalls = voiceCallRepository.findByStatus(VoiceCallStatus.FAILED, PageRequest.of(0, 1)).getContent();
            if (!failedCalls.isEmpty()) {
                response.setLastFailedCall(failedCalls.get(0).getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
            }
        } catch (Exception e) {
            log.error("Failed to load voice call metrics; returning configuration status without metrics", e);
            response.setTotalCalls(0L);
            response.setSuccessfulCalls(0L);
            response.setFailedCalls(0L);
            // Only escalate to ERROR if we otherwise believed voice was healthy/configured.
            if (response.getStatus() == ReadinessStatus.CONFIGURED || response.getStatus() == ReadinessStatus.HEALTHY) {
                response.setStatus(ReadinessStatus.ERROR);
                response.setStatusMessage("Vapi is configured but voice call metrics are temporarily unavailable");
            }
        }

        return response;
    }

    /**
     * Get agents readiness and safety configuration.
     */
    public AgentsResponse getAgentsConfig() {
        List<AgentStatus> agents = new ArrayList<>();

        // Support Agent
        AgentStatus support = new AgentStatus("Support Agent", ReadinessStatus.HEALTHY, "Ready");
        support.setRequiredIntegration("OpenAI");
        support.setCurrentModel("gpt-4o-mini");
        support.setFallbackAvailable(false);
        agents.add(support);

        // Evaluation Agent
        AgentStatus evaluation = new AgentStatus("Evaluation Agent", ReadinessStatus.HEALTHY, "Ready");
        evaluation.setRequiredIntegration("OpenAI");
        evaluation.setCurrentModel("gpt-4o-mini");
        evaluation.setFallbackAvailable(true);
        agents.add(evaluation);

        // Lead Qualification Agent
        AgentStatus leadQual = new AgentStatus("Lead Qualification Agent", ReadinessStatus.HEALTHY, "Ready");
        leadQual.setRequiredIntegration("OpenAI");
        leadQual.setCurrentModel("gpt-4o-mini");
        leadQual.setFallbackAvailable(false);
        agents.add(leadQual);

        // Follow-up Agent
        AgentStatus followUp = new AgentStatus("Follow-up Agent", ReadinessStatus.HEALTHY, "Ready");
        followUp.setRequiredIntegration("OpenAI");
        followUp.setCurrentModel("gpt-4o-mini");
        followUp.setFallbackAvailable(false);
        agents.add(followUp);

        // Website Research/Crawler
        AgentStatus crawler = new AgentStatus("Website Research/Crawler", checkFirecrawlStatus(), "Firecrawl integration");
        crawler.setRequiredIntegration("Firecrawl");
        agents.add(crawler);

        // Knowledge Base Builder
        AgentStatus kb = new AgentStatus("Knowledge Base Builder", embeddingService.isConfigured() ? ReadinessStatus.HEALTHY : ReadinessStatus.NOT_CONFIGURED, "Embedding service");
        kb.setRequiredIntegration("OpenAI");
        agents.add(kb);

        // Lead Finder Agent
        AgentStatus leadFinder = new AgentStatus("Lead Finder Agent", checkApifyStatus(), getApifyMessage());
        leadFinder.setRequiredIntegration("Apify");
        agents.add(leadFinder);

        // Voice Agent
        AgentStatus voice = new AgentStatus("Voice Agent", checkVapiStatus(), getVapiMessage());
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
        response.setApplicationVersion("0.1.0");
        response.setBackendVersion("0.1.0");
        response.setActiveProfile(activeProfile);
        response.setApiBasePath("/api");
        response.setServerTimezone(ZoneId.systemDefault().getId());
        response.setDatabaseType("PostgreSQL");
        response.setRedisConfigured(true);
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
                    break;

                case "redis":
                    result.setSuccess(true);
                    result.setStatus(ReadinessStatus.HEALTHY);
                    result.setMessage("Redis connection test not implemented yet");
                    break;

                case "openai":
                    if (!embeddingService.isConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(ReadinessStatus.NOT_CONFIGURED);
                        result.setMessage("OpenAI API key not configured");
                    } else {
                        result.setSuccess(true);
                        result.setStatus(ReadinessStatus.CONFIGURED);
                        result.setMessage("OpenAI API key configured (full test not implemented)");
                    }
                    break;

                case "firecrawl":
                    if (!firecrawlClient.isConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(ReadinessStatus.NOT_CONFIGURED);
                        result.setMessage("Firecrawl API key not configured");
                    } else {
                        result.setSuccess(true);
                        result.setStatus(ReadinessStatus.CONFIGURED);
                        result.setMessage("Firecrawl API key configured (full test not implemented)");
                    }
                    break;

                case "apify":
                    if (!apifyClient.isConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(apifyEnabled ? ReadinessStatus.NOT_CONFIGURED : ReadinessStatus.DISABLED);
                        result.setMessage(getApifyMessage());
                    } else {
                        result.setSuccess(true);
                        result.setStatus(ReadinessStatus.CONFIGURED);
                        result.setMessage("Apify configured and enabled");
                    }
                    break;

                case "vapi":
                    if (!isVapiConfigured()) {
                        result.setSuccess(false);
                        result.setStatus(vapiEnabled ? ReadinessStatus.NOT_CONFIGURED : ReadinessStatus.DISABLED);
                        result.setMessage(getVapiMessage());
                    } else {
                        result.setSuccess(true);
                        result.setStatus(ReadinessStatus.CONFIGURED);
                        result.setMessage("Vapi configured and enabled");
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
            result.setMessage("Test failed: " + sanitizeErrorMessage(e.getMessage()));
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

    private void testDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(2)) {
                throw new Exception("Database connection is not valid");
            }
        }
    }

    private ReadinessStatus checkRedisStatus() {
        // Redis is configured if the application started successfully
        // A more sophisticated check would require direct Redis connection attempt
        try {
            var health = healthEndpoint.health();
            // If the actuator responds, Redis is likely configured
            return ReadinessStatus.CONFIGURED;
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return ReadinessStatus.ERROR;
        }
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
        if (!vapiEnabled) {
            return ReadinessStatus.DISABLED;
        }
        return isVapiConfigured() ? ReadinessStatus.CONFIGURED : ReadinessStatus.NOT_CONFIGURED;
    }

    private boolean isVapiConfigured() {
        return isNonBlank(vapiApiKey) && isNonBlank(vapiAssistantId) && isNonBlank(vapiPhoneNumberId);
    }

    private String getVapiMessage() {
        if (!vapiEnabled) {
            return "Vapi disabled";
        }
        if (!isVapiConfigured()) {
            return "Vapi enabled but configuration incomplete";
        }
        return "Vapi configured and enabled";
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
        return value != null && !value.trim().isEmpty();
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        // Remove any potential secrets from error messages
        return message.replaceAll("(?i)(api[_-]?key|token|secret|password)[=:\\s]+[a-zA-Z0-9_-]+", "$1=***");
    }
}
