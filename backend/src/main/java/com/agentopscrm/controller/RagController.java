package com.agentopscrm.controller;

import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.service.KnowledgeBaseService;
import com.agentopscrm.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Knowledge Base build + RAG search operations (F-004).
 *
 * Endpoints:
 *  - POST /api/businesses/{id}/knowledge-base/build
 *  - POST /api/rag/search
 *
 * Business isolation is enforced in the service/repository layer; the businessId
 * from the request is only ever used to scope queries, never to bypass them.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
@RestController
@RequestMapping("/api")
public class RagController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final RagService ragService;

    public RagController(KnowledgeBaseService knowledgeBaseService, RagService ragService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.ragService = ragService;
    }

    /**
     * Build the knowledge base for a business.
     * POST /api/businesses/{id}/knowledge-base/build
     */
    @PostMapping("/businesses/{id}/knowledge-base/build")
    public ResponseEntity<ApiResponse<BuildResponse>> buildKnowledgeBase(@PathVariable UUID id) {
        try {
            KnowledgeBaseService.BuildResult result = knowledgeBaseService.buildKnowledgeBase(id);

            BuildResponse response = new BuildResponse(
                    result.getBusinessId() != null ? result.getBusinessId().toString() : id.toString(),
                    result.isSuccess(),
                    result.getStatus(),
                    result.getDocumentsProcessed(),
                    result.getChunksCreated(),
                    result.getEmbeddingsCreated(),
                    result.getSkipped(),
                    result.getMessage());

            if (!result.isSuccess()) {
                // Configuration / provider failures are surfaced as a 500 with a clean body.
                return ResponseEntity.status(500).body(ApiResponse.error(result.getMessage()));
            }
            return ResponseEntity.ok(ApiResponse.success(response, result.getMessage()));

        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to build knowledge base: " + e.getMessage()));
        }
    }

    /**
     * Search the knowledge base using RAG.
     * POST /api/rag/search
     */
    @PostMapping("/rag/search")
    public ResponseEntity<ApiResponse<SearchResponse>> searchKnowledgeBase(@RequestBody SearchRequest request) {
        if (request == null || request.getBusinessId() == null || request.getBusinessId().isBlank()) {
            return ResponseEntity.status(400).body(ApiResponse.error("businessId is required"));
        }
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.status(400).body(ApiResponse.error("query is required"));
        }

        UUID businessId;
        try {
            businessId = UUID.fromString(request.getBusinessId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error("businessId is not a valid UUID"));
        }

        try {
            RagService.SearchResult result = ragService.search(businessId, request.getQuery(), request.getTopK());

            List<RagResultItem> items = result.getResults().stream()
                    .map(this::toItem)
                    .collect(Collectors.toList());

            SearchResponse response = new SearchResponse(
                    result.getQuery(),
                    result.getTotalResults(),
                    items);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (RagService.RagSearchException e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Search failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }

    /**
     * Retrieval-Augmented answer: grounded LLM answer + sources + retrieved chunks.
     * POST /api/rag/answer
     */
    @PostMapping("/rag/answer")
    public ResponseEntity<ApiResponse<AnswerResponse>> answer(@RequestBody SearchRequest request) {
        if (request == null || request.getBusinessId() == null || request.getBusinessId().isBlank()) {
            return ResponseEntity.status(400).body(ApiResponse.error("businessId is required"));
        }
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.status(400).body(ApiResponse.error("query is required"));
        }

        UUID businessId;
        try {
            businessId = UUID.fromString(request.getBusinessId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error("businessId is not a valid UUID"));
        }

        try {
            RagService.AnswerResult result = ragService.answer(businessId, request.getQuery(), request.getTopK());

            List<RagResultItem> items = result.getResults().stream()
                    .map(this::toItem)
                    .collect(Collectors.toList());

            AnswerResponse response = new AnswerResponse(
                    result.getBusinessId(),
                    result.getQuery(),
                    result.getAnswer(),
                    result.getSources(),
                    items,
                    result.getTopK(),
                    result.getStatus());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (RagService.RagSearchException e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Answer failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Answer failed: " + e.getMessage()));
        }
    }

    private RagResultItem toItem(RagService.RagResult r) {
        return new RagResultItem(
                r.getChunkId(),
                r.getContent(),
                r.getSourceUrl(),
                r.getDocumentTitle(),
                r.getRank(),
                r.getSimilarity());
    }

    // ===== DTOs =====

    /** Response for the knowledge-base build endpoint. */
    public static class BuildResponse {
        private final String businessId;
        private final boolean success;
        private final String status;
        private final int documentsProcessed;
        private final int chunksCreated;
        private final int embeddingsCreated;
        private final int skipped;
        private final String message;

        public BuildResponse(String businessId, boolean success, String status, int documentsProcessed,
                             int chunksCreated, int embeddingsCreated, int skipped, String message) {
            this.businessId = businessId;
            this.success = success;
            this.status = status;
            this.documentsProcessed = documentsProcessed;
            this.chunksCreated = chunksCreated;
            this.embeddingsCreated = embeddingsCreated;
            this.skipped = skipped;
            this.message = message;
        }

        public String getBusinessId() { return businessId; }
        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public int getDocumentsProcessed() { return documentsProcessed; }
        public int getChunksCreated() { return chunksCreated; }
        public int getEmbeddingsCreated() { return embeddingsCreated; }
        public int getSkipped() { return skipped; }
        public String getMessage() { return message; }
    }

    /** Request body for RAG search. */
    public static class SearchRequest {
        private String businessId;
        private String query;
        private Integer topK;

        public String getBusinessId() { return businessId; }
        public void setBusinessId(String businessId) { this.businessId = businessId; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
        // Alias so clients may send "limit" instead of "topK".
        public void setLimit(Integer limit) { this.topK = limit; }
    }

    /** Response for RAG search. */
    public static class SearchResponse {
        private final String query;
        private final int totalResults;
        private final List<RagResultItem> items;

        public SearchResponse(String query, int totalResults, List<RagResultItem> items) {
            this.query = query;
            this.totalResults = totalResults;
            this.items = items;
        }

        public String getQuery() { return query; }
        public int getTotalResults() { return totalResults; }
        public List<RagResultItem> getItems() { return items; }
    }

    /** A single matching chunk. */
    public static class RagResultItem {
        private final String chunkId;
        private final String content;
        private final String sourceUrl;
        private final String documentTitle;
        private final int rank;
        private final Double similarity;

        public RagResultItem(String chunkId, String content, String sourceUrl,
                            String documentTitle, int rank, Double similarity) {
            this.chunkId = chunkId;
            this.content = content;
            this.sourceUrl = sourceUrl;
            this.documentTitle = documentTitle;
            this.rank = rank;
            this.similarity = similarity;
        }

        public String getChunkId() { return chunkId; }
        public String getContent() { return content; }
        public String getSourceUrl() { return sourceUrl; }
        public String getDocumentTitle() { return documentTitle; }
        public int getRank() { return rank; }
        public Double getSimilarity() { return similarity; }
    }

    /** Response for the RAG answer endpoint. */
    public static class AnswerResponse {
        private final String businessId;
        private final String query;
        private final String answer;
        private final List<String> sources;
        private final List<RagResultItem> results;
        private final int topK;
        private final String status;

        public AnswerResponse(String businessId, String query, String answer, List<String> sources,
                             List<RagResultItem> results, int topK, String status) {
            this.businessId = businessId;
            this.query = query;
            this.answer = answer;
            this.sources = sources;
            this.results = results;
            this.topK = topK;
            this.status = status;
        }

        public String getBusinessId() { return businessId; }
        public String getQuery() { return query; }
        public String getAnswer() { return answer; }
        public List<String> getSources() { return sources; }
        public List<RagResultItem> getResults() { return results; }
        public int getTopK() { return topK; }
        public String getStatus() { return status; }
    }
}
