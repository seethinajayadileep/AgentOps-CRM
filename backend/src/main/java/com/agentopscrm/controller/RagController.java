package com.agentopscrm.controller;

import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.dto.KnowledgeBaseJobResponse;
import com.agentopscrm.entity.Business;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.repository.KnowledgeChunkRepository;
import com.agentopscrm.service.KnowledgeBaseJobService;
import com.agentopscrm.service.KnowledgeBaseService;
import com.agentopscrm.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for Knowledge Base build + RAG search operations (F-004, Bug 2 fix).
 *
 * Endpoints:
 *  - POST /api/businesses/{id}/knowledge-base/build (202 Accepted - async job)
 *  - GET  /api/businesses/{id}/knowledge-base/jobs/{jobId}
 *  - GET  /api/businesses/{id}/knowledge-base/jobs/active
 *  - POST /api/rag/search
 *
 * Business isolation is enforced in the service/repository layer; the businessId
 * from the request is only ever used to scope queries, never to bypass them.
 *
 * Bug 2 fix: the build endpoint used to run synchronously and could exceed
 * frontend/proxy timeouts even though the backend eventually completed the
 * work. It now enqueues a background job and returns immediately; clients
 * poll the job status endpoint for real progress.
 *
 * @author AgentOps Team
 * @version 0.4.0
 */
@RestController
@RequestMapping("/api")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseJobService knowledgeBaseJobService;
    private final RagService ragService;
    private final BusinessRepository businessRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    public RagController(KnowledgeBaseService knowledgeBaseService,
                         KnowledgeBaseJobService knowledgeBaseJobService,
                         RagService ragService,
                         BusinessRepository businessRepository,
                         DocumentRepository documentRepository,
                         KnowledgeChunkRepository knowledgeChunkRepository) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseJobService = knowledgeBaseJobService;
        this.ragService = ragService;
        this.businessRepository = businessRepository;
        this.documentRepository = documentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
    }

    /**
     * Start an asynchronous knowledge-base build for a business.
     * POST /api/businesses/{id}/knowledge-base/build
     *
     * Always returns 202 Accepted immediately (never blocks on the actual
     * crawl/chunk/embed work) with the job id, businessId, status and
     * startedAt timestamp. Duplicate active builds for the same business are
     * prevented - the existing in-flight job is returned instead of starting
     * a new one.
     */
    @PostMapping("/businesses/{id}/knowledge-base/build")
    public ResponseEntity<ApiResponse<KnowledgeBaseJobResponse>> buildKnowledgeBase(@PathVariable UUID id) {
        try {
            KnowledgeBaseJobResponse job = knowledgeBaseJobService.startBuild(id);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(job, "Knowledge base build job accepted"));
        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to start knowledge base build job for business {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to start knowledge base build: " + e.getMessage()));
        }
    }

    /**
     * Poll the status/progress of a knowledge-base build job.
     * GET /api/businesses/{businessId}/knowledge-base/jobs/{jobId}
     */
    @GetMapping("/businesses/{businessId}/knowledge-base/jobs/{jobId}")
    public ResponseEntity<ApiResponse<KnowledgeBaseJobResponse>> getKnowledgeBaseJob(
            @PathVariable UUID businessId, @PathVariable UUID jobId) {
        try {
            KnowledgeBaseJobResponse job = knowledgeBaseJobService.getJob(jobId);
            if (!businessId.equals(job.getBusinessId())) {
                return ResponseEntity.status(404).body(ApiResponse.error("Job not found for this business"));
            }
            return ResponseEntity.ok(ApiResponse.success(job));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to fetch knowledge base job {}", jobId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch job: " + e.getMessage()));
        }
    }

    /**
     * Restore the active (in-progress) build job for a business, if any -
     * used by the frontend to resume polling after a page refresh.
     * GET /api/businesses/{businessId}/knowledge-base/jobs/active
     */
    @GetMapping("/businesses/{businessId}/knowledge-base/jobs/active")
    public ResponseEntity<ApiResponse<KnowledgeBaseJobResponse>> getActiveKnowledgeBaseJob(
            @PathVariable UUID businessId) {
        try {
            return knowledgeBaseJobService.getActiveJob(businessId)
                    .map(job -> ResponseEntity.ok(ApiResponse.success(job)))
                    .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null, "No active build job")));
        } catch (Exception e) {
            log.error("Failed to fetch active knowledge base job for business {}", businessId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch active job: " + e.getMessage()));
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

            RagService.RagDiagnostics diag = result.getDiagnostics();
            DiagnosticsInfo diagnostics = diag != null ? new DiagnosticsInfo(
                    diag.getDocumentsCount(),
                    diag.getTotalChunks(),
                    diag.getLegacyEmbeddedChunks(),
                    diag.getPgvectorEmbeddedChunks(),
                    diag.getRetrievalMode(),
                    diag.getRejectionReasons()
            ) : null;
            
            AnswerResponse response = new AnswerResponse(
                    result.getBusinessId(),
                    result.getQuery(),
                    result.getAnswer(),
                    result.getSources(),
                    items,
                    result.getTopK(),
                    result.getStatus(),
                    diagnostics);

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
        private final DiagnosticsInfo diagnostics;

        public AnswerResponse(String businessId, String query, String answer, List<String> sources,
                             List<RagResultItem> results, int topK, String status, DiagnosticsInfo diagnostics) {
            this.businessId = businessId;
            this.query = query;
            this.answer = answer;
            this.sources = sources;
            this.results = results;
            this.topK = topK;
            this.status = status;
            this.diagnostics = diagnostics;
        }

        public String getBusinessId() { return businessId; }
        public String getQuery() { return query; }
        public String getAnswer() { return answer; }
        public List<String> getSources() { return sources; }
        public List<RagResultItem> getResults() { return results; }
        public int getTopK() { return topK; }
        public String getStatus() { return status; }
        public DiagnosticsInfo getDiagnostics() { return diagnostics; }
    }

    /** Diagnostics information for RAG operations. */
    public static class DiagnosticsInfo {
        private final long documentsCount;
        private final long totalChunks;
        private final long legacyEmbeddedChunks;
        private final long pgvectorEmbeddedChunks;
        private final String retrievalMode;
        private final List<String> rejectionReasons;

        public DiagnosticsInfo(long documentsCount, long totalChunks, long legacyEmbeddedChunks,
                               long pgvectorEmbeddedChunks, String retrievalMode, List<String> rejectionReasons) {
            this.documentsCount = documentsCount;
            this.totalChunks = totalChunks;
            this.legacyEmbeddedChunks = legacyEmbeddedChunks;
            this.pgvectorEmbeddedChunks = pgvectorEmbeddedChunks;
            this.retrievalMode = retrievalMode;
            this.rejectionReasons = rejectionReasons;
        }

        public long getDocumentsCount() { return documentsCount; }
        public long getTotalChunks() { return totalChunks; }
        public long getLegacyEmbeddedChunks() { return legacyEmbeddedChunks; }
        public long getPgvectorEmbeddedChunks() { return pgvectorEmbeddedChunks; }
        public String getRetrievalMode() { return retrievalMode; }
        public List<String> getRejectionReasons() { return rejectionReasons; }
    }

    /**
     * Get diagnostics for a business's knowledge base.
     * GET /api/businesses/{id}/knowledge-base/diagnostics
     */
    @GetMapping("/businesses/{id}/knowledge-base/diagnostics")
    public ResponseEntity<ApiResponse<KnowledgeBaseDiagnostics>> getKnowledgeBaseDiagnostics(@PathVariable UUID id) {
        try {
            Business business = businessRepository.findById(id)
                    .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + id));
            
            long totalChunks = knowledgeChunkRepository.countByBusinessId(id);
            long pgvectorChunks = knowledgeChunkRepository.countByBusinessIdWithPgvectorEmbedding(id);
            long legacyChunks = totalChunks - pgvectorChunks;
            long documentsCount = documentRepository.countByBusinessId(id);
            
            boolean ready = totalChunks > 0 && pgvectorChunks > 0;
            String status = totalChunks == 0 ? "NO_CHUNKS" : 
                           (pgvectorChunks == 0 ? "NO_EMBEDDINGS" : "READY");
            
            KnowledgeBaseDiagnostics diagnostics = new KnowledgeBaseDiagnostics(
                    business.getName(),
                    documentsCount,
                    totalChunks,
                    legacyChunks,
                    pgvectorChunks,
                    ready,
                    status
            );
            
            return ResponseEntity.ok(ApiResponse.success(diagnostics));
            
        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve diagnostics: " + e.getMessage()));
        }
    }

    /** Knowledge base diagnostics response. */
    public static class KnowledgeBaseDiagnostics {
        private final String businessName;
        private final long documentsCount;
        private final long totalChunks;
        private final long legacyEmbeddedChunks;
        private final long pgvectorEmbeddedChunks;
        private final boolean ready;
        private final String status;

        public KnowledgeBaseDiagnostics(String businessName, long documentsCount, long totalChunks,
                                        long legacyEmbeddedChunks, long pgvectorEmbeddedChunks,
                                        boolean ready, String status) {
            this.businessName = businessName;
            this.documentsCount = documentsCount;
            this.totalChunks = totalChunks;
            this.legacyEmbeddedChunks = legacyEmbeddedChunks;
            this.pgvectorEmbeddedChunks = pgvectorEmbeddedChunks;
            this.ready = ready;
            this.status = status;
        }

        public String getBusinessName() { return businessName; }
        public long getDocumentsCount() { return documentsCount; }
        public long getTotalChunks() { return totalChunks; }
        public long getLegacyEmbeddedChunks() { return legacyEmbeddedChunks; }
        public long getPgvectorEmbeddedChunks() { return pgvectorEmbeddedChunks; }
        public boolean isReady() { return ready; }
        public String getStatus() { return status; }
    }
}
