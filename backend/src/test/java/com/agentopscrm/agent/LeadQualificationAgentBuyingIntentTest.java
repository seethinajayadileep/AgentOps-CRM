package com.agentopscrm.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void commonInterestedTyposAreBuyingIntent() {
        assertTrue(agent.detectBuyingIntent("i am intersted in your service"));
        assertTrue(agent.detectBuyingIntent("I am intrested in SEO"));
    }

    @Test
    void wantYourServiceIsBuyingIntent() {
        assertTrue(agent.detectBuyingIntent("I want your service"));
    }

    @Test
    void emailAndPhoneLookLikeContactDetails() {
        assertTrue(agent.looksLikeContactDetails("jayadileepb@gmail.com and +918328270668"));
        assertFalse(agent.looksLikeContactDetails("what are the service you will provide"));
    }

    @Test
    void regexFillsMissingEmailAndPhone() {
        LeadQualificationAgent.LeadExtractionResult result = new LeadQualificationAgent.LeadExtractionResult();
        LeadQualificationAgent.applyRegexContactHints(
                result, "jayadileepb@gmail.com and +918328270668");
        assertEquals("jayadileepb@gmail.com", result.getEmail());
        assertEquals("+918328270668", result.getPhone());
    }
}
