package com.agentopscrm.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadQualificationAgentBuyingIntentTest {

    private LeadQualificationAgent agent;

    @BeforeEach
    void setUp() throws Exception {
        agent = new LeadQualificationAgent(
                "",
                "gpt-4o-mini",
                new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8)),
                new RestTemplate(),
                new ObjectMapper());
    }

    @Test
    void knowledgeQuestionIsNotBuyingIntent() {
        assertFalse(agent.detectBuyingIntent("I need business hours for Saturday"));
    }

    @Test
    void quoteRequestIsBuyingIntent() {
        assertTrue(agent.detectBuyingIntent("Please send a quote for your package"));
    }
}
