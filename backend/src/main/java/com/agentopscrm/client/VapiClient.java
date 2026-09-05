package com.agentopscrm.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Client for interacting with Vapi.ai voice call API.
 * 
 * Why exists: Encapsulates all HTTP communication with Vapi API for voice call operations.
 * 
 * @author AgentOps Team
 * @version 0.2.0
 */
@Component
public class VapiClient {

    private static final Logger logger = LoggerFactory.getLogger(VapiClient.class);
    private static final String VAPI_API_BASE_URL = "https://api.vapi.ai";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final boolean enabled;

    public VapiClient(
        RestTemplate restTemplate,
        @Value("${vapi.api-key}") String apiKey,
        @Value("${vapi.enabled:false}") boolean enabled
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    /**
     * Start a new outbound phone call via Vapi.
     *
     * @param request the call request parameters
     * @return the Vapi call response with call ID
     * @throws VapiException if the API call fails
     */
    public VapiCallResponse startCall(VapiCallRequest request) throws VapiException {
        if (!enabled) {
            throw new VapiException("Vapi integration is disabled");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new VapiException("Vapi API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<VapiCallRequest> entity = new HttpEntity<>(request, headers);

        try {
            logger.info("Starting Vapi call to phone number: {}", request.customer != null ? request.customer.number : "unknown");
            
            ResponseEntity<VapiCallResponse> response = restTemplate.exchange(
                VAPI_API_BASE_URL + "/call/phone",
                HttpMethod.POST,
                entity,
                VapiCallResponse.class
            );

            VapiCallResponse callResponse = response.getBody();
            if (callResponse == null) {
                throw new VapiException("Received null response from Vapi API");
            }

            logger.info("Successfully started Vapi call with ID: {}", callResponse.id);
            return callResponse;

        } catch (HttpClientErrorException e) {
            logger.error("Vapi API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new VapiException("Vapi API client error: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Vapi API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new VapiException("Vapi API server error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling Vapi API", e);
            throw new VapiException("Unexpected error calling Vapi API: " + e.getMessage(), e);
        }
    }

    /**
     * Lightweight assistant lookup. Never starts a phone call.
     */
    public void pingAssistant(String assistantId) throws VapiException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new VapiException("Vapi API key is not configured");
        }
        if (assistantId == null || assistantId.isBlank()) {
            throw new VapiException("Vapi assistant id is not configured");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    VAPI_API_BASE_URL + "/assistant/" + assistantId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new VapiException("Unexpected status: " + response.getStatusCode().value());
            }
        } catch (VapiException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            throw new VapiException("Vapi rejected the stored credentials", e);
        } catch (Exception e) {
            throw new VapiException("Vapi health check failed", e);
        }
    }

    /**
     * Get details of an existing call by ID.
     *
     * @param callId the Vapi call ID
     * @return the call details
     * @throws VapiException if the API call fails
     */
    public VapiCallResponse getCall(String callId) throws VapiException {
        if (!enabled) {
            throw new VapiException("Vapi integration is disabled");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new VapiException("Vapi API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            logger.info("Fetching Vapi call details for ID: {}", callId);
            
            ResponseEntity<VapiCallResponse> response = restTemplate.exchange(
                VAPI_API_BASE_URL + "/call/" + callId,
                HttpMethod.GET,
                entity,
                VapiCallResponse.class
            );

            VapiCallResponse callResponse = response.getBody();
            if (callResponse == null) {
                throw new VapiException("Received null response from Vapi API");
            }

            logger.info("Successfully fetched Vapi call details for ID: {}", callId);
            return callResponse;

        } catch (HttpClientErrorException e) {
            logger.error("Vapi API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new VapiException("Vapi API client error: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Vapi API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new VapiException("Vapi API server error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling Vapi API", e);
            throw new VapiException("Unexpected error calling Vapi API: " + e.getMessage(), e);
        }
    }

    /**
     * Download recording bytes from a provider URL. The Vapi token is only sent
     * to Vapi hosts. Cloudflare R2 rejects {@code Authorization: Bearer} with 400.
     * Never logs the URL.
     */
    public ResponseEntity<byte[]> downloadMedia(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "AgentOpsCRM/0.2");
        if (shouldSendVapiAuth(url) && apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
        return restTemplate.exchange(
            URI.create(url),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            byte[].class
        );
    }

    /**
     * Download a call recording via Vapi's authenticated artifact endpoints.
     * Private R2 / hipaa-recordings object URLs are not directly downloadable;
     * {@code GET /call/{id}/mono-recording} 302s to a short-lived signed URL.
     */
    public ResponseEntity<byte[]> downloadCallRecording(String vapiCallId) throws VapiException {
        if (vapiCallId == null || vapiCallId.isBlank()) {
            throw new VapiException("Vapi call id is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new VapiException("Vapi API key is not configured");
        }

        Exception last = null;
        for (String artifact : java.util.List.of("mono-recording", "stereo-recording")) {
            URI uri = URI.create(VAPI_API_BASE_URL + "/call/" + vapiCallId + "/" + artifact);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.set(HttpHeaders.USER_AGENT, "AgentOpsCRM/0.2");
                headers.setAccept(java.util.List.of(MediaType.ALL));
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );
                if (response.getBody() != null && response.getBody().length > 0) {
                    return response;
                }
            } catch (HttpClientErrorException.NotFound e) {
                last = e;
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                String body = e.getResponseBodyAsString();
                logger.warn("Vapi {} artifact failed status={} detail={}",
                        artifact,
                        e.getStatusCode().value(),
                        body == null ? "" : body.substring(0, Math.min(180, body.length())));
                last = e;
            }
        }
        throw new VapiException("Recording artifact could not be retrieved", last);
    }

    static boolean shouldSendVapiAuth(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return false;
            }
            String lower = host.toLowerCase();
            return lower.equals("api.vapi.ai") || lower.equals("storage.vapi.ai") || lower.endsWith(".vapi.ai");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Request object for starting a Vapi call.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VapiCallRequest {
        @JsonProperty("customer")
        public CustomerInfo customer;

        @JsonProperty("assistantId")
        public String assistantId;

        @JsonProperty("phoneNumberId")
        public String phoneNumberId;

        public VapiCallRequest() {}

        public VapiCallRequest(String phoneNumber, String assistantId, String phoneNumberId) {
            this.customer = new CustomerInfo(phoneNumber);
            this.assistantId = assistantId;
            this.phoneNumberId = phoneNumberId;
        }
    }

    /**
     * Customer info for Vapi call request.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerInfo {
        @JsonProperty("number")
        public String number;

        public CustomerInfo() {}

        public CustomerInfo(String number) {
            this.number = number;
        }
    }

    /**
     * Response object from Vapi call API.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VapiCallResponse {
        @JsonProperty("id")
        public String id;

        @JsonProperty("status")
        public String status;

        @JsonProperty("customer")
        public CustomerInfo customer;

        @JsonProperty("transcript")
        public String transcript;

        @JsonProperty("summary")
        public String summary;

        @JsonProperty("recordingUrl")
        public String recordingUrl;

        @JsonProperty("duration")
        public Integer duration;

        @JsonProperty("startedAt")
        public String startedAt;

        @JsonProperty("endedAt")
        public String endedAt;

        @JsonProperty("endedReason")
        public String endedReason;

        @JsonProperty("error")
        public String error;

        // Vapi nests transcript/recording under "artifact" and the AI summary
        // under "analysis" in the GET /call response. Capture both so we can
        // populate the call record when syncing status directly from the API.
        @JsonProperty("artifact")
        public Artifact artifact;

        @JsonProperty("analysis")
        public Analysis analysis;

        public VapiCallResponse() {}
        
        // Helper method to get phone number from customer object
        public String getPhoneNumber() {
            return customer != null ? customer.number : null;
        }
    }

    /**
     * Artifact data nested in the Vapi call response (recording + transcript).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Artifact {
        @JsonProperty("transcript")
        public String transcript;

        @JsonProperty("recordingUrl")
        public String recordingUrl;

        @JsonProperty("stereoRecordingUrl")
        public String stereoRecordingUrl;

        @JsonProperty("presignedMonoUrl")
        public String presignedMonoUrl;

        @JsonProperty("presignedStereoUrl")
        public String presignedStereoUrl;

        public Artifact() {}
    }

    /**
     * Analysis data nested in the Vapi call response (AI-generated summary).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Analysis {
        @JsonProperty("summary")
        public String summary;

        public Analysis() {}
    }

    /**
     * Custom exception for Vapi client errors.
     */
    public static class VapiException extends Exception {
        public VapiException(String message) {
            super(message);
        }

        public VapiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
