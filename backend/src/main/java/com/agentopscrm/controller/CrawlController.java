package com.agentopscrm.controller;

import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.entity.Document;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.service.CrawlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for website crawling operations.
 *
 * API IDs: API-010, API-012
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@RestController
@RequestMapping("/api")
public class CrawlController {

    private final CrawlService crawlService;

    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    /**
     * Start crawling a business's website.
     * Endpoint: POST /api/businesses/{id}/crawl (API-010)
     *
     * @param id The business ID
     * @return ApiResponse with crawl result
     */
    @PostMapping("/businesses/{id}/crawl")
    public ResponseEntity<ApiResponse<CrawlResponse>> startCrawl(@PathVariable UUID id) {
        try {
            CrawlService.CrawlResult result = crawlService.startCrawl(id);

            CrawlResponse response = new CrawlResponse(
                    result.getStatus().name(),
                    result.getMessage()
            );

            return ResponseEntity.ok(ApiResponse.success(response, result.getMessage()));

        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.<CrawlResponse>error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.<CrawlResponse>error("Crawl failed: " + e.getMessage()));
        }
    }

    /**
     * Get all documents for a business.
     * Endpoint: GET /api/businesses/{id}/documents (API-012)
     *
     * @param id The business ID
     * @return List of documents
     */
    @GetMapping("/businesses/{id}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable UUID id) {
        List<Document> documents = crawlService.getBusinessDocuments(id);

        List<DocumentResponse> responses = documents.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Convert Document entity to DTO.
     */
    private DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
                doc.getId().toString(),
                doc.getUrl(),
                doc.getTitle(),
                doc.getStatus() != null ? doc.getStatus().name() : "UNKNOWN",
                doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null,
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null
        );
    }

    // ===== DTOs =====

    public static class CrawlResponse {
        private final String status;
        private final String message;

        public CrawlResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class DocumentResponse {
        private final String id;
        private final String url;
        private final String title;
        private final String status;
        private final String createdAt;
        private final String updatedAt;

        public DocumentResponse(String id, String url, String title, String status,
                              String createdAt, String updatedAt) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }
}