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

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * Slack incoming webhook for fresh-lead alerts. There is no side-effect-free
 * ping on incoming webhooks — Settings Test Connection only validates the URL.
 */
@Component
public class SlackWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(SlackWebhookClient.class);

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public SlackWebhookClient(
            RestTemplate restTemplate,
            @Value("${slack.webhook-url:}") String webhookUrl) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }

    public boolean isConfigured() {
        return isSlackWebhook(webhookUrl);
    }

    public void sendMessage(String text) throws SlackException {
        if (!isConfigured()) {
            throw new SlackException("Slack is not configured. Set SLACK_WEBHOOK_URL.");
        }
        if (text == null || text.isBlank()) {
            throw new SlackException("Slack message text is empty");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("text", text), headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SlackException("Unexpected status: " + response.getStatusCode().value());
            }
            log.debug("Slack fresh-lead message sent");
        } catch (SlackException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new SlackException("Failed to send Slack message", e);
        } catch (Exception e) {
            throw new SlackException("Failed to send Slack message", e);
        }
    }

    static boolean isSlackWebhook(String value) {
        if (!IntegrationSecrets.isHttpsUrl(value)) {
            return false;
        }
        try {
            String host = URI.create(value.trim()).getHost();
            return host != null && host.toLowerCase(Locale.ROOT).endsWith("hooks.slack.com");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static class SlackException extends Exception {
        public SlackException(String message) {
            super(message);
        }

        public SlackException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
