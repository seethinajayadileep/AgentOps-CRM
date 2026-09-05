package com.agentopscrm.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

import com.agentopscrm.util.FollowUpEmailBody;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client for Resend's email API. Used to send approved follow-up messages.
 * Never starts a send from {@link #ping()} — that only authenticates.
 */
@Component
public class ResendClient {

    private static final Logger log = LoggerFactory.getLogger(ResendClient.class);
    static final String EMAILS_URL = "https://api.resend.com/emails";
    static final String DOMAINS_URL = "https://api.resend.com/domains";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String from;

    public ResendClient(
            RestTemplate restTemplate,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String from) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from == null ? "" : from.trim();
    }

    public boolean isConfigured() {
        return isUsableSecret(apiKey) && fromContainsAt(from);
    }

    public String getFrom() {
        return from;
    }

    /**
     * Authenticated GET /domains. Send-only keys return 403 restricted_api_key;
     * that still means the key is valid, so ping succeeds.
     */
    public void ping() throws ResendException {
        if (!isConfigured()) {
            throw new ResendException("Resend is not configured. Set RESEND_API_KEY and RESEND_FROM.");
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    DOMAINS_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResendException("Unexpected status: " + response.getStatusCode().value());
            }
        } catch (ResendException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            if (isSendOnlyKey(e)) {
                log.info("Resend ping: send-only API key accepted");
                return;
            }
            throw toResendException("Resend health check failed", e);
        } catch (Exception e) {
            throw new ResendException("Resend health check failed", e);
        }
    }

    public SendResult sendEmail(String to, String subject, String text) throws ResendException {
        if (!isConfigured()) {
            throw new ResendException("Resend is not configured. Set RESEND_API_KEY and RESEND_FROM.");
        }
        if (to == null || to.isBlank()) {
            throw new ResendException("Recipient email is missing");
        }
        String plain = FollowUpEmailBody.plain(text);
        Map<String, Object> body = new HashMap<>();
        body.put("from", from);
        body.put("to", List.of(to.trim()));
        body.put("subject", subject == null || subject.isBlank() ? "Follow-up" : subject.trim());
        body.put("text", plain);
        body.put("html", FollowUpEmailBody.html(text));

        try {
            ResponseEntity<SendResponse> response = restTemplate.exchange(
                    EMAILS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders()),
                    SendResponse.class);
            SendResponse payload = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || payload == null || payload.id == null || payload.id.isBlank()) {
                throw new ResendException("Resend did not return a message id");
            }
            return new SendResult(payload.id);
        } catch (ResendException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw toResendException("Failed to send email", e);
        } catch (Exception e) {
            throw new ResendException("Failed to send email", e);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private static boolean isUsableSecret(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.startsWith("your_")
                && !trimmed.contains("_here")
                && !trimmed.equals("...")
                && !trimmed.startsWith("re_...");
    }

    private static boolean fromContainsAt(String value) {
        return value != null && value.contains("@");
    }

    private static boolean isSendOnlyKey(HttpStatusCodeException e) {
        if (e.getStatusCode().value() != 403) {
            return false;
        }
        String body = e.getResponseBodyAsString();
        if (body == null) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("restricted_api_key") || lower.contains("only send emails");
    }

    private static ResendException toResendException(String prefix, HttpStatusCodeException e) {
        String detail = extractMessage(e.getResponseBodyAsString());
        boolean unauthorized = e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403;
        String message = detail == null || detail.isBlank() ? prefix : prefix + ": " + detail;
        ResendException ex = new ResendException(message, e);
        ex.unauthorized = unauthorized && !isSendOnlyKey(e);
        return ex;
    }

    private static String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        int key = body.indexOf("\"message\"");
        if (key < 0) {
            return body.length() > 240 ? body.substring(0, 240) : body;
        }
        int colon = body.indexOf(':', key);
        int start = body.indexOf('"', colon + 1);
        int end = body.indexOf('"', start + 1);
        if (start < 0 || end < 0) {
            return body.length() > 240 ? body.substring(0, 240) : body;
        }
        return body.substring(start + 1, end);
    }

    public record SendResult(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SendResponse {
        public String id;
    }

    public static class ResendException extends Exception {
        public boolean unauthorized;

        public ResendException(String message) {
            super(message);
        }

        public ResendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
