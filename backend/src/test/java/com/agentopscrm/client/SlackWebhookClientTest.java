package com.agentopscrm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackWebhookClientTest {

    @Mock private RestTemplate restTemplate;

    @Test
    void isConfiguredRequiresSlackWebhookHost() {
        assertFalse(new SlackWebhookClient(restTemplate, "").isConfigured());
        assertFalse(new SlackWebhookClient(restTemplate, "https://example.com/hook").isConfigured());
        assertTrue(new SlackWebhookClient(
                restTemplate, "https://hooks.slack.com/services/T/B/xxx").isConfigured());
    }

    @Test
    void sendMessagePostsText() throws Exception {
        String url = "https://hooks.slack.com/services/T/B/xxx";
        SlackWebhookClient client = new SlackWebhookClient(restTemplate, url);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        client.sendMessage("New lead · Acme");

        verify(restTemplate).exchange(eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }
}
