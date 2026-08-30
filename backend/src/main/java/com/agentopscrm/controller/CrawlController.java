package com.agentopscrm.controller;

import com.agentopscrm.dto.ApiResponse;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.service.CrawlService;
import com.agentopscrm.util.SafeErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for website crawling operations.
 */
@RestController
@RequestMapping("/api")
public class CrawlController {

    private final CrawlService crawlService;

    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @PostMapping("/businesses/{id}/crawl")
    public ResponseEntity<ApiResponse<CrawlResponse>> startCrawl(@PathVariable UUID id) {
        try {
            CrawlService.CrawlResult result = crawlService.startCrawl(id);
            CrawlResponse response = toCrawlResponse(result);
            HttpStatus status = result.getStatus() != null && result.getStatus().isActive()
                    ? HttpStatus.ACCEPTED : HttpStatus.OK;
            return ResponseEntity.status(status).body(ApiResponse.success(response, result.getMessage()));
        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.<CrawlResponse>error("Business not found."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.<CrawlResponse>error(SafeErrorMessages.CRAWL_FAILED));
        }
    }

    @GetMapping("/businesses/{id}/crawl-status")
    public ResponseEntity<ApiResponse<CrawlResponse>> getCrawlStatus(@PathVariable UUID id) {
        try {
            Business business = crawlService.getBusinessForCrawl(id);
            CrawlService.CrawlResult result = new CrawlService.CrawlResult(
                    true, publicMessage(business), business.getCrawlStatus(), business);
            return ResponseEntity.ok(ApiResponse.success(toCrawlResponse(result)));
        } catch (BusinessNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.<CrawlResponse>error("Business not found."));
        }
    }

    @GetMapping("/businesses/{id}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable UUID id) {
        List<Document> documents = crawlService.getBusinessDocuments(id);
        List<DocumentResponse> responses = documents.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private CrawlResponse toCrawlResponse(CrawlService.CrawlResult result) {
        Business business = result.getBusiness();
        CrawlStatus publicStatus = result.getStatus() != null
                ? result.getStatus().toPublicStatus() : CrawlStatus.NOT_STARTED;
        return new CrawlResponse(
                publicStatus.name(),
                result.getMessage(),
                business != null && business.getCrawlStartedAt() != null
                        ? business.getCrawlStartedAt().toString() : null,
                business != null && business.getCrawlFinishedAt() != null
                        ? business.getCrawlFinishedAt().toString() : null,
                business != null && business.getCrawlError() != null && !business.getCrawlError().isBlank()
                        ? SafeErrorMessages.sanitize(business.getCrawlError()) : null,
                business != null ? business.getCrawlPagesSaved() : 0,
                business != null ? business.getCrawlPagesTotal() : 0,
                result.getElapsedSeconds()
        );
    }

    private String publicMessage(Business business) {
        CrawlStatus status = business.getCrawlStatus() != null
                ? business.getCrawlStatus().toPublicStatus() : CrawlStatus.NOT_STARTED;
        return switch (status) {
            case QUEUED -> "Crawl is queued.";
            case CRAWLING -> "Crawl is running.";
            case COMPLETED -> "Crawl completed.";
            case FAILED -> business.getCrawlError() != null
                    ? SafeErrorMessages.sanitize(business.getCrawlError()) : SafeErrorMessages.CRAWL_FAILED;
            default -> "Crawl has not started.";
        };
    }

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

    public static class CrawlResponse {
        private final String status;
        private final String message;
        private final String startedAt;
        private final String finishedAt;
        private final String error;
        private final int pagesSaved;
        private final int pagesTotal;
        private final Long elapsedSeconds;

        public CrawlResponse(String status, String message) {
            this(status, message, null, null, null, 0, 0, null);
        }

        public CrawlResponse(String status, String message, String startedAt, String finishedAt,
                             String error, int pagesSaved, int pagesTotal, Long elapsedSeconds) {
            this.status = status;
            this.message = message;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.error = error;
            this.pagesSaved = pagesSaved;
            this.pagesTotal = pagesTotal;
            this.elapsedSeconds = elapsedSeconds;
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getStartedAt() { return startedAt; }
        public String getFinishedAt() { return finishedAt; }
        public String getError() { return error; }
        public int getPagesSaved() { return pagesSaved; }
        public int getPagesTotal() { return pagesTotal; }
        public Long getElapsedSeconds() { return elapsedSeconds; }
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
