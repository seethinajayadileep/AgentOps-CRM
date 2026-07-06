package com.agentopscrm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating embeddings using OpenAI API.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public EmbeddingService(
            @Value("${openai.api-key:}") String apiKey,
            RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    /**
     * Generate embeddings for a list of texts.
     *
     * @param texts List of texts to embed
     * @return List of embedding arrays
     * @throws EmbeddingException if embedding fails
     */
    public List<float[]> generateEmbeddings(List<String> texts) throws EmbeddingException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmbeddingException("OPENAI_API_KEY is not configured");
        }

        // Batch embed (up to 2048 texts per request, but we'll limit to 10)
        List<float[]> embeddings = new ArrayList<>();
        int batchSize = 10;

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            embeddings.addAll(generateBatchEmbeddings(batch));
        }

        log.info("Generated {} embeddings", embeddings.size());
        return embeddings;
    }

    /**
     * Generate embedding for a single text.
     *
     * @param text The text to embed
     * @return Embedding array
     * @throws EmbeddingException if embedding fails
     */
    public float[] generateEmbedding(String text) throws EmbeddingException {
        List<String> texts = List.of(text);
        List<float[]> embeddings = generateEmbeddings(texts);
        return embeddings.get(0);
    }

    private List<float[]> generateBatchEmbeddings(List<String> texts) throws EmbeddingException {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", texts);
            requestBody.put("model", "text-embedding-3-small"); // 1536 dimensions

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + apiKey);
            headers.put("Content-Type", "application/json");

            Map<String, Object> requestEntity = new HashMap<>();
            requestEntity.put("headers", headers);
            requestEntity.put("body", requestBody);

            // Using RestTemplate
            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            httpHeaders.set("Authorization", "Bearer " + apiKey);
            httpHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(requestBody, httpHeaders);

            var response = restTemplate.postForObject(
                    OPENAI_API_URL,
                    entity,
                    Map.class
            );

            if (response == null) {
                throw new EmbeddingException("No response from OpenAI API");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

            List<float[]> embeddings = new ArrayList<>();
            for (Map<String, Object> item : data) {
                List<Number> embeddingList = (List<Number>) item.get("embedding");
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }
                embeddings.add(embedding);
            }

            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate embeddings", e);
            throw new EmbeddingException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }

    /**
     * Check if OpenAI is configured.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Get embedding dimension.
     */
    public int getEmbeddingDimension() {
        return 1536; // text-embedding-3-small
    }

    /**
     * Custom exception for embedding errors.
     */
    public static class EmbeddingException extends Exception {
        public EmbeddingException(String message) {
            super(message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}