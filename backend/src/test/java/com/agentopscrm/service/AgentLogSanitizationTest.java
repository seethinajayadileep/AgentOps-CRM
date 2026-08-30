package com.agentopscrm.service;

import com.agentopscrm.dto.AgentLogResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.repository.AgentLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLogSanitizationTest {

    @Mock private AgentLogRepository agentLogRepository;
    private AgentLogService service;

    @BeforeEach
    void setUp() {
        service = new AgentLogService(agentLogRepository);
    }

    @Test
    void getById_sanitizesPkixAndUrlsAndAddsOperatorFields() {
        UUID id = UUID.randomUUID();
        AgentLog log = new AgentLog();
        log.setId(id);
        log.setAgentName("Crawler");
        log.setAction("CRAWL_FAILED");
        log.setStatus(AgentActionStatus.ERROR);
        log.setErrorMessage("PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException https://api.firecrawl.dev/v1/crawl");
        log.setOutputJson("{\"error\":\"javax.net.ssl.SSLHandshakeException token=sk-secret\"}");
        when(agentLogRepository.findById(id)).thenReturn(Optional.of(log));

        AgentLogResponse response = service.getAgentLogById(id);

        assertFalse(response.getErrorMessage().toLowerCase().contains("pkix"));
        assertFalse(response.getErrorMessage().contains("https://"));
        assertFalse(response.getErrorMessage().contains("sun.security"));
        assertNotNull(response.getCorrelationId());
        assertNotNull(response.getErrorCategory());
        assertNotNull(response.getRecommendedAction());
        assertFalse(response.getOutputJson().contains("https://"));
        assertFalse(response.getOutputJson().contains("sk-secret"));
    }
}
