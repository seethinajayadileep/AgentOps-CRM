package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.client.FirecrawlClient;
import com.agentopscrm.client.VapiClient;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for Bug 3: Voice Settings API ("Error Loading Data").
 *
 * Root cause: GET /api/settings/voice could surface a raw exception (and
 * therefore a bare 500) whenever Vapi was disabled/misconfigured or the
 * voice call metrics query failed, instead of returning a safe readiness
 * response. These tests assert that missing/invalid Vapi environment
 * variables and repository failures always yield a 200-safe response with
 * an appropriate {@link ReadinessStatus}, never an unhandled exception.
 */
@ExtendWith(MockitoExtension.class)
class SettingsServiceVoiceConfigTest {

    @Mock private DataSource dataSource;
    @Mock private HealthEndpoint healthEndpoint;
    @Mock private FirecrawlClient firecrawlClient;
    @Mock private VapiClient vapiClient;
    @Mock private ApifyClient apifyClient;
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
                dataSource, healthEndpoint, firecrawlClient, vapiClient, apifyClient,
                embeddingService, businessRepository, documentRepository,
                knowledgeChunkRepository, voiceCallRepository, agentLogRepository);
    }

    private void setVapiConfig(boolean enabled, String apiKey, String assistantId,
                                String phoneNumberId, String webhookSecret) {
        ReflectionTestUtils.setField(settingsService, "vapiEnabled", enabled);
        ReflectionTestUtils.setField(settingsService, "vapiApiKey", apiKey);
        ReflectionTestUtils.setField(settingsService, "vapiAssistantId", assistantId);
        ReflectionTestUtils.setField(settingsService, "vapiPhoneNumberId", phoneNumberId);
        ReflectionTestUtils.setField(settingsService, "vapiWebhookSecret", webhookSecret);
    }

    @SuppressWarnings("unchecked")
    private void stubHappyRepositories() {
        when(voiceCallRepository.count()).thenReturn(0L);
        when(voiceCallRepository.countByStatus(any())).thenReturn(0L);
        Page<Object> emptyPage = new PageImpl<>(java.util.Collections.emptyList());
        when(voiceCallRepository.findByStatus(any(), any())).thenReturn((Page) emptyPage);
    }

    @Test
    void getVoiceConfig_whenVapiDisabled_returnsDisabledStatus_noException() {
        setVapiConfig(false, "", "", "", "");
        stubHappyRepositories();

        VoiceConfigResponse response = assertDoesNotThrow(() -> settingsService.getVoiceConfig());

        assertEquals(ReadinessStatus.DISABLED, response.getStatus());
        assertFalse(response.isEnabled());
        assertEquals("Vapi disabled", response.getStatusMessage());
    }

    @Test
    void getVoiceConfig_whenEnabledButMissingVariables_returnsNotConfigured_noException() {
        // VAPI_ENABLED=true but VAPI_API_KEY / VAPI_ASSISTANT_ID / VAPI_PHONE_NUMBER_ID missing
        setVapiConfig(true, "", "", "", "");
        stubHappyRepositories();

        VoiceConfigResponse response = assertDoesNotThrow(() -> settingsService.getVoiceConfig());

        assertEquals(ReadinessStatus.NOT_CONFIGURED, response.getStatus());
        assertTrue(response.isEnabled());
        assertFalse(response.isApiKeyConfigured());
        assertFalse(response.isAssistantIdConfigured());
        assertFalse(response.isPhoneNumberIdConfigured());
        assertEquals("Vapi enabled but configuration incomplete", response.getStatusMessage());
    }

    @Test
    void getVoiceConfig_whenEnabledWithPartialConfig_returnsNotConfigured() {
        // Only API key set, assistant/phone number missing - still NOT_CONFIGURED
        setVapiConfig(true, "sk-test-key", "", "", "");
        stubHappyRepositories();

        VoiceConfigResponse response = assertDoesNotThrow(() -> settingsService.getVoiceConfig());

        assertEquals(ReadinessStatus.NOT_CONFIGURED, response.getStatus());
        assertTrue(response.isApiKeyConfigured());
        assertFalse(response.isAssistantIdConfigured());
    }

    @Test
    void getVoiceConfig_whenFullyConfigured_returnsConfiguredStatus() {
        setVapiConfig(true, "sk-test-key", "assistant-123", "phone-456", "webhook-secret");
        stubHappyRepositories();

        VoiceConfigResponse response = assertDoesNotThrow(() -> settingsService.getVoiceConfig());

        assertEquals(ReadinessStatus.CONFIGURED, response.getStatus());
        assertTrue(response.isApiKeyConfigured());
        assertTrue(response.isAssistantIdConfigured());
        assertTrue(response.isPhoneNumberIdConfigured());
        assertTrue(response.isWebhookSecretConfigured());
        assertEquals("Vapi configured and enabled", response.getStatusMessage());
    }

    @Test
    void getVoiceConfig_neverLeaksSecretValues() {
        setVapiConfig(true, "super-secret-key", "assistant-123", "phone-456", "webhook-secret-value");
        stubHappyRepositories();

        VoiceConfigResponse response = settingsService.getVoiceConfig();

        // Response must only contain booleans/status, never the raw secret values.
        assertTrue(response.isApiKeyConfigured());
        // No getter on the DTO exposes the raw key/secret - compile-time guarantee,
        // but assert the status message also does not embed secret material.
        assertFalse(response.getStatusMessage().contains("super-secret-key"));
        assertFalse(response.getStatusMessage().contains("webhook-secret-value"));
    }

    @Test
    void getVoiceConfig_whenVoiceCallMetricsQueryFails_stillReturnsSafeResponse_noException() {
        setVapiConfig(true, "sk-test-key", "assistant-123", "phone-456", "webhook-secret");
        when(voiceCallRepository.count()).thenThrow(new RuntimeException("DB connection lost"));

        VoiceConfigResponse response = assertDoesNotThrow(() -> settingsService.getVoiceConfig());

        assertNotNull(response);
        assertEquals(ReadinessStatus.ERROR, response.getStatus());
        assertEquals(0L, response.getTotalCalls());
        assertEquals(0L, response.getSuccessfulCalls());
        assertEquals(0L, response.getFailedCalls());
    }

    @Test
    void getVoiceConfig_whenDisabledAndMetricsQueryFails_remainsDisabled_noException() {
        // Disabled status should not be escalated to ERROR just because metrics failed.
        setVapiConfig(false, "", "", "", "");
        when(voiceCallRepository.count()).thenThrow(new RuntimeException("DB connection lost"));

        VoiceConfigResponse response = assertDoesNotThrow(() -> settingsService.getVoiceConfig());

        assertEquals(ReadinessStatus.DISABLED, response.getStatus());
    }
}
