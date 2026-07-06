package com.agentopscrm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with the Apify platform API (F-010 Apify Lead Finder).
 *
 * Why exists: Isolates ALL Apify-specific HTTP communication and payload shape. The rest
 * of the system consumes only the stable, normalized {@link ApifyLeadResult} shape, so the
 * underlying Apify actor can be swapped later without touching services or controllers.
 *
 * Configuration is optional: if {@code apify.enabled=false} or the token is missing, the
 * application still starts and calls fail with a clean, user-facing message.
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@Component
public class ApifyClient {

    private static final Logger logger = LoggerFactory.getLogger(ApifyClient.class);
    private static final String APIFY_API_BASE_URL = "https://api.apify.com/v2";
    public static final String NOT_CONFIGURED_MESSAGE = "Apify is not configured.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiToken;
    private final boolean enabled;
    private final String defaultActorId;

    public ApifyClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Value("${apify.api-token:}") String apiToken,
        @Value("${apify.enabled:false}") boolean enabled,
        @Value("${apify.default-actor-id:}") String defaultActorId
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
        this.enabled = enabled;
        this.defaultActorId = defaultActorId;
    }

    /**
     * @return true when Apify integration is enabled AND an API token is present.
     */
    public boolean isConfigured() {
        return enabled && apiToken != null && !apiToken.trim().isEmpty();
    }

    /**
     * @return the configured default actor id (may be null/blank).
     */
    public String getDefaultActorId() {
        return defaultActorId;
    }

    /**
     * Start an Apify actor run.
     *
     * @param actorId  the Apify actor id (falls back to the configured default when blank)
     * @param industry search industry filter
     * @param location search location filter
     * @param keywords search keywords
     * @param maxResults optional cap on the number of results
     * @return metadata about the started run (run id, dataset id, status)
     * @throws ApifyException if not configured or the API call fails
     */
    public ApifyRunInfo startActorRun(String actorId, String industry, String location,
                                      String keywords, Integer maxResults) throws ApifyException {
        ensureConfigured();

        String resolvedActor = (actorId != null && !actorId.trim().isEmpty()) ? actorId.trim() : defaultActorId;
        if (resolvedActor == null || resolvedActor.trim().isEmpty()) {
            throw new ApifyException("No Apify actor id provided and no default actor configured.");
        }

        // Apify actor ids in the API path use "~" instead of "/" (e.g. user~actor-name).
        String actorPath = resolvedActor.replace("/", "~");

        Map<String, Object> input = buildActorInput(industry, location, keywords, maxResults);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(input, headers);

        String url = UriComponentsBuilder.fromHttpUrl(APIFY_API_BASE_URL)
            .path("/acts/{actorId}/runs")
            .queryParam("token", apiToken)
            .buildAndExpand(actorPath)
            .toUriString();

        try {
            logger.info("Starting Apify actor run for actor: {}", resolvedActor);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return parseRunInfo(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Apify API client error starting run: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApifyException("Apify API client error: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Apify API server error starting run: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApifyException("Apify API server error: " + e.getMessage(), e);
        } catch (ApifyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error starting Apify actor run", e);
            throw new ApifyException("Unexpected error starting Apify actor run: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch the current status of an Apify run.
     *
     * @param runId the Apify run id
     * @return run info with the latest status and dataset id
     * @throws ApifyException if not configured or the API call fails
     */
    public ApifyRunInfo getRun(String runId) throws ApifyException {
        ensureConfigured();

        String url = UriComponentsBuilder.fromHttpUrl(APIFY_API_BASE_URL)
            .path("/actor-runs/{runId}")
            .queryParam("token", apiToken)
            .buildAndExpand(runId)
            .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            return parseRunInfo(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Apify API error fetching run {}: {} - {}", runId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApifyException("Apify API error fetching run: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching Apify run {}", runId, e);
            throw new ApifyException("Unexpected error fetching Apify run: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch and normalize the items produced by an Apify run's dataset.
     *
     * @param datasetId the Apify dataset id
     * @return list of normalized {@link ApifyLeadResult}
     * @throws ApifyException if not configured or the API call fails
     */
    @SuppressWarnings("unchecked")
    public List<ApifyLeadResult> fetchDatasetItems(String datasetId) throws ApifyException {
        ensureConfigured();

        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new ApifyException("No Apify dataset id available for this run yet.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(APIFY_API_BASE_URL)
            .path("/datasets/{datasetId}/items")
            .queryParam("token", apiToken)
            .queryParam("clean", true)
            .queryParam("format", "json")
            .buildAndExpand(datasetId)
            .toUriString();

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, List.class);
            List<Object> rawItems = response.getBody();
            List<ApifyLeadResult> results = new ArrayList<>();
            if (rawItems == null) {
                return results;
            }
            for (Object raw : rawItems) {
                if (raw instanceof Map) {
                    results.add(normalizeItem((Map<String, Object>) raw));
                }
            }
            logger.info("Fetched {} items from Apify dataset {}", results.size(), datasetId);
            return results;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Apify API error fetching dataset {}: {} - {}", datasetId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApifyException("Apify API error fetching dataset: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching Apify dataset {}", datasetId, e);
            throw new ApifyException("Unexpected error fetching Apify dataset: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers (Apify-specific shape lives entirely in this class)
    // ------------------------------------------------------------------

    private void ensureConfigured() throws ApifyException {
        if (!isConfigured()) {
            throw new ApifyException(NOT_CONFIGURED_MESSAGE);
        }
    }

    /**
     * Build a generic actor input map. Different actors accept different keys, so we send a
     * broad set of commonly used field names to maximise compatibility.
     */
    private Map<String, Object> buildActorInput(String industry, String location,
                                                 String keywords, Integer maxResults) {
        Map<String, Object> input = new HashMap<>();

        StringBuilder query = new StringBuilder();
        if (keywords != null && !keywords.isBlank()) {
            query.append(keywords.trim());
        }
        if (industry != null && !industry.isBlank()) {
            if (query.length() > 0) query.append(' ');
            query.append(industry.trim());
        }
        if (location != null && !location.isBlank()) {
            if (query.length() > 0) query.append(" in ");
            query.append(location.trim());
        }
        String combined = query.toString().trim();

        if (!combined.isEmpty()) {
            input.put("search", combined);
            input.put("query", combined);
            input.put("queries", List.of(combined));
            input.put("searchStringsArray", List.of(combined));
        }
        if (industry != null && !industry.isBlank()) {
            input.put("industry", industry.trim());
            input.put("category", industry.trim());
        }
        if (location != null && !location.isBlank()) {
            input.put("location", location.trim());
            input.put("locationQuery", location.trim());
            input.put("city", location.trim());
        }
        if (keywords != null && !keywords.isBlank()) {
            input.put("keywords", keywords.trim());
        }
        if (maxResults != null && maxResults > 0) {
            input.put("maxResults", maxResults);
            input.put("maxItems", maxResults);
            input.put("maxCrawledPlaces", maxResults);
            // compass/crawler-google-places caps results with these two keys; without them the
            // actor uses a very large default and over-charges. Keep both for compatibility.
            input.put("maxCrawledPlacesPerSearch", maxResults);
            input.put("maxReviews", 0);
            input.put("maxImages", 0);
            input.put("resultsLimit", maxResults);
        }
        return input;
    }

    @SuppressWarnings("unchecked")
    private ApifyRunInfo parseRunInfo(Map<String, Object> body) throws ApifyException {
        if (body == null) {
            throw new ApifyException("Received empty response from Apify API");
        }
        Map<String, Object> data = body.containsKey("data") && body.get("data") instanceof Map
            ? (Map<String, Object>) body.get("data")
            : body;

        ApifyRunInfo info = new ApifyRunInfo();
        info.runId = asString(data.get("id"));
        info.status = asString(data.get("status"));
        info.datasetId = asString(data.get("defaultDatasetId"));
        return info;
    }

    /**
     * Normalize a raw Apify dataset item into the stable {@link ApifyLeadResult} shape.
     * Handles the most common field naming variations across actors.
     */
    private ApifyLeadResult normalizeItem(Map<String, Object> raw) {
        ApifyLeadResult result = new ApifyLeadResult();

        result.businessName = firstNonBlank(raw, "businessName", "name", "title", "companyName", "company");
        result.websiteUrl = firstNonBlank(raw, "website", "websiteUrl", "url", "domain", "link");
        result.contactName = firstNonBlank(raw, "contactName", "personName", "ownerName", "fullName", "contact");
        result.email = firstNonBlank(raw, "email", "emailAddress", "contactEmail");
        result.phone = firstNonBlank(raw, "phone", "phoneNumber", "telephone", "contactPhone", "mobile");
        result.location = firstNonBlank(raw, "location", "address", "city", "fullAddress", "formattedAddress");
        result.industry = firstNonBlank(raw, "industry", "category", "categoryName", "type");
        result.sourceUrl = firstNonBlank(raw, "sourceUrl", "url", "link", "profileUrl", "detailUrl");

        // Handle emails/phones provided as arrays.
        if (result.email == null) {
            result.email = firstFromArray(raw, "emails");
        }
        if (result.phone == null) {
            result.phone = firstFromArray(raw, "phones", "phoneNumbers");
        }

        try {
            result.rawDataJson = objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            result.rawDataJson = String.valueOf(raw);
        }
        return result;
    }

    private String firstNonBlank(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String s = String.valueOf(value).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String firstFromArray(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first != null) {
                    String s = String.valueOf(first).trim();
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Metadata about an Apify run.
     */
    public static class ApifyRunInfo {
        public String runId;
        public String status;
        public String datasetId;

        public boolean isFinished() {
            return "SUCCEEDED".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status)
                || "ABORTED".equalsIgnoreCase(status)
                || "TIMED-OUT".equalsIgnoreCase(status);
        }

        public boolean isSucceeded() {
            return "SUCCEEDED".equalsIgnoreCase(status);
        }
    }

    /**
     * Stable, normalized representation of a single discovered prospect. The rest of the
     * codebase depends only on this shape, never on raw Apify payloads.
     */
    public static class ApifyLeadResult {
        public String businessName;
        public String websiteUrl;
        public String contactName;
        public String email;
        public String phone;
        public String location;
        public String industry;
        public String sourceUrl;
        public String rawDataJson;
    }

    /**
     * Custom exception for Apify client errors.
     */
    public static class ApifyException extends Exception {
        public ApifyException(String message) {
            super(message);
        }

        public ApifyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
