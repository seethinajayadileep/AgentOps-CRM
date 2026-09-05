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

/**
 * Cal.com booking link used in fresh-lead notifications. Optional API key is
 * only for Settings Test Connection ({@code GET /v2/me}); ping never creates a booking.
 */
@Component
public class CalComClient {

    private static final Logger log = LoggerFactory.getLogger(CalComClient.class);
    static final String ME_URL = "https://api.cal.com/v2/me";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String bookingUrl;

    public CalComClient(
            RestTemplate restTemplate,
            @Value("${calcom.api-key:}") String apiKey,
            @Value("${calcom.booking-url:}") String bookingUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.bookingUrl = bookingUrl == null ? "" : bookingUrl.trim();
    }

    public boolean isConfigured() {
        return IntegrationSecrets.isHttpsUrl(bookingUrl);
    }

    public boolean hasApiKey() {
        return IntegrationSecrets.isUsableSecret(apiKey);
    }

    public String getBookingUrl() {
        return isConfigured() ? bookingUrl : null;
    }

    public void ping() throws CalComException {
        if (!hasApiKey()) {
            throw new CalComException("Cal.com API key is not set. Booking URL is enough for lead pings.");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey);
            ResponseEntity<String> response = restTemplate.exchange(
                    ME_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new CalComException("Unexpected status: " + response.getStatusCode().value());
            }
            log.debug("Cal.com ping succeeded");
        } catch (CalComException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new CalComException("Cal.com health check failed", e);
        } catch (Exception e) {
            throw new CalComException("Cal.com health check failed", e);
        }
    }

    public static class CalComException extends Exception {
        public CalComException(String message) {
            super(message);
        }

        public CalComException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
