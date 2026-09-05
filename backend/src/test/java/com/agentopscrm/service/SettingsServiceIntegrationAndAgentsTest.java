package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.dto.settings.AgentStatus;
import com.agentopscrm.dto.settings.IntegrationStatus;
import com.agentopscrm.dto.settings.IntegrationTestResult;
import com.agentopscrm.dto.settings.VoiceConfigResponse;
import com.agentopscrm.entity.enums.ReadinessStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.repository.KnowledgeChunkRepository;
import com.agentopscrm.repository.VoiceCallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import com.agentopscrm.util.AppVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceIntegrationAndAgentsTest {

    @Mock private DataSource dataSource;
    @Mock private HealthEndpoint healthEndpoint;
    @Mock private com.agentopscrm.client.FirecrawlClient firecrawlClient;
    @Mock private com.agentopscrm.client.VapiClient vapiClient;
    @Mock private ApifyClient apifyClient;
    @Mock private com.agentopscrm.client.ResendClient resendClient;
    @Mock private com.agentopscrm.client.CalComClient calComClient;
    @Mock private com.agentopscrm.client.TelegramClient telegramClient;
    @Mock private com.agentopscrm.client.SlackWebhookClient slackWebhookClient;
    @Mock private EmbeddingService embeddingService;
    @Mock private BusinessRepository businessRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock private VoiceCallRepository voiceCallRepository;
    @Mock private AgentLogRepository agentLogRepository;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(
                dataSource, healthEndpoint, firecrawlClient, vapiClient, apifyClient, resendClient,
                calComClient, telegramClient, slackWebhookClient,
                embeddingService, businessRepository, documentRepository,
                knowledgeChunkRepository, voiceCallRepository, agentLogRepository);
        ReflectionTestUtils.setField(settingsService, "apifyEnabled", true);
    }

    @Test
    void apifyTestConnectionPingsProviderAndReportsHealthy() throws Exception {
        when(apifyClient.isConfigured()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("apify");

        verify(apifyClient).ping();
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.HEALTHY, result.getStatus());
        assertTrue(result.getMessage().contains("CONNECTED"));
        assertNotNull(result.getTestedAt());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    void apifyTestConnectionReportsDegradedOnTlsFailure() throws Exception {
        when(apifyClient.isConfigured()).thenReturn(true);
        ApifyClient.ApifyException tls = new ApifyClient.ApifyException(
                "Unexpected error testing Apify connection",
                new javax.net.ssl.SSLHandshakeException("PKIX path building failed"));
        doThrow(tls).when(apifyClient).ping();

        IntegrationTestResult result = settingsService.testIntegration("apify");

        assertFalse(result.isSuccess());
        assertEquals(ReadinessStatus.DEGRADED, result.getStatus());
        assertTrue(result.getMessage().contains("DEGRADED"));
        assertFalse(result.getMessage().toLowerCase().contains("pkix"));
    }

    @Test
    void agentCardsExposeNameAndPurpose() {
        when(apifyClient.isConfigured()).thenReturn(false);
        when(firecrawlClient.isConfigured()).thenReturn(false);
        when(embeddingService.isConfigured()).thenReturn(true);

        List<AgentStatus> agents = settingsService.getAgentsConfig().getAgents();
        assertEquals(8, agents.size());
        for (AgentStatus agent : agents) {
            assertNotNull(agent.getName());
            assertFalse(agent.getName().isBlank());
            assertNotNull(agent.getMessage());
            assertFalse(agent.getMessage().isBlank());
            assertNotNull(agent.getRequiredIntegration());
        }
    }

    @Test
    void agentJsonExposesNameAndPurposeFieldsForTheFrontend() throws Exception {
        when(apifyClient.isConfigured()).thenReturn(false);
        when(firecrawlClient.isConfigured()).thenReturn(false);
        when(embeddingService.isConfigured()).thenReturn(true);

        JsonNode json = new ObjectMapper().valueToTree(settingsService.getAgentsConfig());
        assertEquals(8, json.get("agents").size());
        for (JsonNode agent : json.get("agents")) {
            assertTrue(agent.hasNonNull("name") && !agent.get("name").asText().isBlank());
            assertTrue(agent.hasNonNull("message") && !agent.get("message").asText().isBlank());
            assertTrue(agent.has("requiredIntegration"));
            assertTrue(agent.has("currentModel"));
            assertTrue(agent.has("fallbackAvailable"));
        }
    }

    @Test
    void applicationVersionIsAuthoritative() {
        assertEquals("0.2.0", AppVersion.VALUE);
    }

    @Test
    void apifyTestConnectionReportsFailedOnUnauthorized() throws Exception {
        when(apifyClient.isConfigured()).thenReturn(true);
        ApifyClient.ApifyException unauthorized = new ApifyClient.ApifyException("unauthorized");
        unauthorized.unauthorized = true;
        doThrow(unauthorized).when(apifyClient).ping();

        IntegrationTestResult result = settingsService.testIntegration("apify");

        assertFalse(result.isSuccess());
        assertEquals(ReadinessStatus.ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("FAILED"));
    }

    @Test
    void vapiIsConfiguredWhenCredentialsExistEvenIfFlagDisabled() {
        ReflectionTestUtils.setField(settingsService, "vapiEnabled", false);
        ReflectionTestUtils.setField(settingsService, "vapiApiKey", "key");
        ReflectionTestUtils.setField(settingsService, "vapiAssistantId", "asst");
        ReflectionTestUtils.setField(settingsService, "vapiPhoneNumberId", "phone");

        VoiceConfigResponse voice = settingsService.getVoiceConfig();
        assertEquals(ReadinessStatus.CONFIGURED, voice.getStatus());
        assertTrue(voice.isEnabled());
        assertFalse(voice.getStatus().name().equals("DISABLED"));
        assertTrue(voice.getStatusMessage().toLowerCase().contains("credentials"));
    }

    @Test
    void openaiAgentsAreNotHealthyWhenOpenAiIsMissing() {
        when(embeddingService.isConfigured()).thenReturn(false);
        when(firecrawlClient.isConfigured()).thenReturn(false);
        when(apifyClient.isConfigured()).thenReturn(false);

        List<AgentStatus> agents = settingsService.getAgentsConfig().getAgents();
        AgentStatus support = agents.stream().filter(a -> a.getName().contains("Support")).findFirst().orElseThrow();
        AgentStatus evaluation = agents.stream().filter(a -> a.getName().contains("Evaluation")).findFirst().orElseThrow();
        AgentStatus followUp = agents.stream().filter(a -> a.getName().contains("Follow-up")).findFirst().orElseThrow();

        assertEquals(ReadinessStatus.NOT_CONFIGURED, support.getStatus());
        assertEquals(ReadinessStatus.DEGRADED, evaluation.getStatus());
        assertEquals(ReadinessStatus.NOT_CONFIGURED, followUp.getStatus());
    }

    @Test
    void redisTestDoesNotReportHealthyWhenDisabled() throws Exception {
        IntegrationTestResult result = settingsService.testIntegration("redis");
        assertFalse(result.isSuccess());
        assertEquals(ReadinessStatus.DISABLED, result.getStatus());
        assertFalse(result.getMessage().toLowerCase().contains("healthy"));
        assertFalse(settingsService.getSystemDiagnostics().isRedisConfigured());

        when(embeddingService.isConfigured()).thenReturn(false);
        when(firecrawlClient.isConfigured()).thenReturn(false);
        when(apifyClient.isConfigured()).thenReturn(false);
        Connection conn = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);

        IntegrationStatus redis = settingsService.getIntegrations().getIntegrations().stream()
                .filter(item -> "Redis".equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertFalse(redis.isConfigured());
        assertEquals(ReadinessStatus.DISABLED, redis.getStatus());
        assertEquals("Not connected", redis.getConfigDetails());
    }

    @Test
    void redisDiagnosticsMatchIntegrationsWhenEnabled() throws Exception {
        org.springframework.data.redis.core.StringRedisTemplate template =
                mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        org.springframework.data.redis.connection.RedisConnectionFactory factory =
                mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
        org.springframework.data.redis.connection.RedisConnection connection =
                mock(org.springframework.data.redis.connection.RedisConnection.class);
        when(template.getConnectionFactory()).thenReturn(factory);
        when(template.getRequiredConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        settingsService.setRedisTemplate(template);
        ReflectionTestUtils.setField(settingsService, "redisEnabled", true);

        assertTrue(settingsService.getSystemDiagnostics().isRedisConfigured());

        when(embeddingService.isConfigured()).thenReturn(false);
        when(firecrawlClient.isConfigured()).thenReturn(false);
        when(apifyClient.isConfigured()).thenReturn(false);
        Connection conn = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);

        IntegrationStatus redis = settingsService.getIntegrations().getIntegrations().stream()
                .filter(item -> "Redis".equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertTrue(redis.isConfigured());
        assertEquals(ReadinessStatus.HEALTHY, redis.getStatus());
        assertFalse(redis.getConfigDetails().contains("StringRedisTemplate"));
        assertFalse(redis.getConfigDetails().toLowerCase().contains("template"));
        assertTrue(redis.getConfigDetails().contains("REDIS_URL"));

        IntegrationTestResult result = settingsService.testIntegration("redis");
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.HEALTHY, result.getStatus());
    }

    @Test
    void openaiTestConnectionPingsProvider() throws Exception {
        when(embeddingService.isConfigured()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("openai");

        verify(embeddingService).ping();
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONNECTED, result.getStatus());
        assertEquals("LIVE", result.getCheckType());
        assertTrue(result.getMessage().contains("CONNECTED"));
        assertNotNull(result.getTestedAt());
    }

    @Test
    void firecrawlTestConnectionPingsProvider() throws Exception {
        when(firecrawlClient.isConfigured()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("firecrawl");

        verify(firecrawlClient).ping();
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONNECTED, result.getStatus());
        assertEquals("LIVE", result.getCheckType());
    }

    @Test
    void vapiTestConnectionLooksUpAssistantAndNeverStartsACall() throws Exception {
        ReflectionTestUtils.setField(settingsService, "vapiApiKey", "key");
        ReflectionTestUtils.setField(settingsService, "vapiAssistantId", "asst-1");
        ReflectionTestUtils.setField(settingsService, "vapiPhoneNumberId", "phone-1");

        IntegrationTestResult result = settingsService.testIntegration("vapi");

        verify(vapiClient).pingAssistant("asst-1");
        verify(vapiClient, never()).startCall(any());
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONNECTED, result.getStatus());
        assertTrue(result.getMessage().toLowerCase().contains("no call"));
    }

    @Test
    void resendTestConnectionPingsProviderAndNeverSends() throws Exception {
        when(resendClient.isConfigured()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("resend");

        verify(resendClient).ping();
        verify(resendClient, never()).sendEmail(any(), any(), any());
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONNECTED, result.getStatus());
        assertEquals("LIVE", result.getCheckType());
        assertTrue(result.getMessage().toLowerCase().contains("no email"));
    }

    @Test
    void resendHidesOptionalNoteWhenConfigured() throws Exception {
        when(embeddingService.isConfigured()).thenReturn(false);
        when(firecrawlClient.isConfigured()).thenReturn(false);
        when(apifyClient.isConfigured()).thenReturn(false);
        when(resendClient.isConfigured()).thenReturn(true);
        Connection conn = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(2)).thenReturn(true);

        IntegrationStatus resend = settingsService.getIntegrations().getIntegrations().stream()
                .filter(item -> "Resend".equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertTrue(resend.isConfigured());
        assertNull(resend.getConfigDetails());
    }

    @Test
    void telegramTestConnectionPingsBotAndNeverSends() throws Exception {
        when(telegramClient.isConfigured()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("telegram");

        verify(telegramClient).ping();
        verify(telegramClient, never()).sendMessage(any());
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONNECTED, result.getStatus());
        assertEquals("LIVE", result.getCheckType());
        assertTrue(result.getMessage().toLowerCase().contains("no message"));
    }

    @Test
    void slackTestConnectionDoesNotPost() throws Exception {
        when(slackWebhookClient.isConfigured()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("slack");

        verify(slackWebhookClient, never()).sendMessage(any());
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONFIGURED, result.getStatus());
        assertEquals("CONFIGURATION_ONLY", result.getCheckType());
    }

    @Test
    void calComTestConnectionAcceptsDottedName() throws Exception {
        when(calComClient.isConfigured()).thenReturn(true);
        when(calComClient.hasApiKey()).thenReturn(true);

        IntegrationTestResult result = settingsService.testIntegration("cal.com");

        verify(calComClient).ping();
        assertTrue(result.isSuccess());
        assertEquals(ReadinessStatus.CONNECTED, result.getStatus());
        assertTrue(result.getMessage().toLowerCase().contains("no booking"));
    }
}
