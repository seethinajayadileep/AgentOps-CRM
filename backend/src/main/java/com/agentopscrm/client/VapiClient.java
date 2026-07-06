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
