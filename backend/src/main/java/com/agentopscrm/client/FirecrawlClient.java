package com.agentopscrm.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for Firecrawl API - website crawling service.
 *
 * API Documentation: https://docs.firecrawl.dev/apis/api-reference/endpoints/crawl
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Component
public class FirecrawlClient {

    private static final String FIRECRAWL_API_URL = "https://api.firecrawl.dev/v1/crawl";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration MAX_POLL_TIME = Duration.ofMinutes(5);

    private final RestTemplate restTemplate;
    private final String apiKey;

    public FirecrawlClient(
            @Value("${firecrawl.api-key:}") String apiKey,
            RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    /**
     * Start a crawl job for the given URL.
     *
     * @param url The URL to crawl
     * @param limit Maximum number of pages to crawl
     * @return Crawl job response with job ID
     * @throws FirecrawlException if the API call fails
     */
    public FirecrawlCrawlResponse startCrawl(String url, int limit) throws FirecrawlException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new FirecrawlException("FIRECRAWL_API_KEY is not configured");
        }

        // Try v2 format - parameters at root level
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("url", url);
        requestBody.put("limit", limit);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<FirecrawlCrawlResponse> response = restTemplate.postForEntity(
                    FIRECRAWL_API_URL,
                    request,
                    FirecrawlCrawlResponse.class
            );

            if (response.getStatusCode() == HttpStatus.ACCEPTED ||
                response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new FirecrawlException("Unexpected status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new FirecrawlException("Failed to start crawl: " + e.getMessage(), e);
        }
    }

    /**
     * Poll for crawl results until completion or timeout.
     *
     * @param jobId The crawl job ID
     * @return Completed crawl response with page data
     * @throws FirecrawlException if polling fails or times out
     */
    public FirecrawlCrawlResponse pollForResults(String jobId) throws FirecrawlException {
        String pollUrl = FIRECRAWL_API_URL + "/" + jobId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        Instant startTime = Instant.now();

        while (Duration.between(startTime, Instant.now()).compareTo(MAX_POLL_TIME) < 0) {
            try {
                ResponseEntity<FirecrawlCrawlResponse> response = restTemplate.exchange(
                        pollUrl,
                        HttpMethod.GET,
                        request,
                        FirecrawlCrawlResponse.class
                );

                FirecrawlCrawlResponse body = response.getBody();
                if (body == null) {
                    throw new FirecrawlException("Empty response from Firecrawl");
                }

                if ("completed".equals(body.status) || "completed".equals(body.getStatus())) {
                    return body;
                } else if ("failed".equals(body.status) || "failed".equals(body.getStatus())) {
                    throw new FirecrawlException("Crawl job failed: " + body.getError());
                }

                // Still processing, wait and retry
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FirecrawlException("Polling interrupted", e);
            } catch (Exception e) {
                throw new FirecrawlException("Failed to poll for results: " + e.getMessage(), e);
            }
        }

        throw new FirecrawlException("Crawl poll timeout after " + MAX_POLL_TIME);
    }

    /**
     * Execute a complete crawl (start and poll for results).
     *
     * @param url The URL to crawl
     * @param limit Maximum number of pages to crawl
     * @return Completed crawl response with page data
     * @throws FirecrawlException if the crawl fails
     */
    public FirecrawlCrawlResponse executeCrawl(String url, int limit) throws FirecrawlException {
        FirecrawlCrawlResponse startResponse = startCrawl(url, limit);
        return pollForResults(startResponse.getId() != null ? startResponse.getId() : startResponse.getJobId());
    }

    /**
     * Check if Firecrawl is configured.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ===== DTOs for Firecrawl API =====

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FirecrawlCrawlResponse {
        private boolean success;
        private String id;
        private String jobId;
        private String url;
        private String status;
        private String error;
        private Integer total;
        private List<FirecrawlPageData> data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }

        public List<FirecrawlPageData> getData() { return data; }
        public void setData(List<FirecrawlPageData> data) { this.data = data; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FirecrawlPageData {
        private String markdown;
        private String html;
        private FirecrawlPageMetadata metadata;
        private String completedAt;
        private String url;

        public String getMarkdown() { return markdown; }
        public void setMarkdown(String markdown) { this.markdown = markdown; }

        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }

        public FirecrawlPageMetadata getMetadata() { return metadata; }
        public void setMetadata(FirecrawlPageMetadata metadata) { this.metadata = metadata; }

        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FirecrawlPageMetadata {
            private String title;
            private String description;
            private String sourceURL;
            private Integer statusCode;
            private String language;
            private Map<String, Object> additionalProperties = new HashMap<>();

            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }

            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }

            public String getSourceURL() { return sourceURL; }
            public void setSourceURL(String sourceURL) { this.sourceURL = sourceURL; }

            public Integer getStatusCode() { return statusCode; }
            public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

            public String getLanguage() { return language; }
            public void setLanguage(String language) { this.language = language; }
        }
    }

    /**
     * Custom exception for Firecrawl errors.
     */
    public static class FirecrawlException extends Exception {
        public FirecrawlException(String message) {
            super(message);
        }

        public FirecrawlException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}