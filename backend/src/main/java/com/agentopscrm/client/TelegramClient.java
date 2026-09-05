package com.agentopscrm.client;

import com.agentopscrm.util.IntegrationSecrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Telegram Bot API client for fresh-lead alerts. {@link #ping()} calls getMe and
 * never sends a chat message.
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);
    static final String API_BASE = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate;
    private final String botToken;
    private final String chatId;

    public TelegramClient(
            RestTemplate restTemplate,
            @Value("${telegram.bot-token:}") String botToken,
            @Value("${telegram.chat-id:}") String chatId) {
        this.restTemplate = restTemplate;
        this.botToken = botToken == null ? "" : botToken.trim();
        this.chatId = chatId == null ? "" : chatId.trim();
    }

    public boolean isConfigured() {
        return looksLikeBotToken(botToken) && !chatId.isBlank();
    }

    public void ping() throws TelegramException {
        if (!isConfigured()) {
            throw new TelegramException("Telegram is not configured. Set TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID.");
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl("getMe"),
                    HttpMethod.GET,
                    new HttpEntity<>(jsonHeaders()),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new TelegramException("Unexpected status: " + response.getStatusCode().value());
            }
            String body = response.getBody();
            if (body == null || !body.contains("\"ok\":true")) {
                throw new TelegramException("Telegram getMe did not succeed");
            }
        } catch (TelegramException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new TelegramException("Telegram health check failed", e);
        } catch (Exception e) {
            throw new TelegramException("Telegram health check failed", e);
        }
    }

    public void sendMessage(String text) throws TelegramException {
        if (!isConfigured()) {
            throw new TelegramException("Telegram is not configured. Set TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID.");
        }
        if (text == null || text.isBlank()) {
            throw new TelegramException("Telegram message text is empty");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("disable_web_page_preview", true);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl("sendMessage"),
                    HttpMethod.POST,
                    new HttpEntity<>(body, jsonHeaders()),
                    String.class);
            String payload = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || payload == null || !payload.contains("\"ok\":true")) {
                throw new TelegramException("Telegram did not accept the message");
            }
            log.debug("Telegram fresh-lead message sent");
        } catch (TelegramException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new TelegramException("Failed to send Telegram message", e);
        } catch (Exception e) {
            throw new TelegramException("Failed to send Telegram message", e);
        }
    }

    private String apiUrl(String method) {
        return API_BASE + botToken + "/" + method;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    static boolean looksLikeBotToken(String value) {
        if (!IntegrationSecrets.isUsableSecret(value)) {
            return false;
        }
        int colon = value.indexOf(':');
        return colon > 0 && colon < value.length() - 1;
    }

    public static class TelegramException extends Exception {
        public TelegramException(String message) {
            super(message);
        }

        public TelegramException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
