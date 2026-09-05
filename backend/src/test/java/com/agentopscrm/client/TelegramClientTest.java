package com.agentopscrm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramClientTest {

    @Mock private RestTemplate restTemplate;

    @Test
    void isConfiguredRequiresTokenAndChat() {
        assertFalse(new TelegramClient(restTemplate, "", "123").isConfigured());
        assertFalse(new TelegramClient(restTemplate, "123:token", "").isConfigured());
        assertFalse(new TelegramClient(restTemplate, "your_telegram_bot_token_here", "123").isConfigured());
        assertTrue(new TelegramClient(restTemplate, "123456:AAToken", "-1001").isConfigured());
    }

    @Test
    void pingCallsGetMe() throws Exception {
        TelegramClient client = new TelegramClient(restTemplate, "123456:AAToken", "99");
        when(restTemplate.exchange(
                eq("https://api.telegram.org/bot123456:AAToken/getMe"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK));

        client.ping();
    }

    @Test
    void sendMessagePostsToChat() throws Exception {
        TelegramClient client = new TelegramClient(restTemplate, "123456:AAToken", "99");
        when(restTemplate.exchange(
                eq("https://api.telegram.org/bot123456:AAToken/sendMessage"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK));

        client.sendMessage("New lead · Acme");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.telegram.org/bot123456:AAToken/sendMessage"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertTrue(body.get("text").toString().contains("New lead"));
        assertTrue(Boolean.TRUE.equals(body.get("disable_web_page_preview")));
    }
}
