package com.agentopscrm.service;

import com.agentopscrm.client.FirecrawlClient;
import com.agentopscrm.client.FirecrawlClient.FirecrawlException;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import com.agentopscrm.util.SafeErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Website crawl orchestration. Status is committed immediately; Firecrawl work
 * runs on {@link CrawlAsyncRunner}.
 */
@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private static final int MAX_PAGES_TO_CRAWL = 30;

    private final BusinessRepository businessRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private final FirecrawlClient firecrawlClient;
    private final CrawlStateWriter crawlStateWriter;
    private final CrawlAsyncRunner crawlAsyncRunner;

    public CrawlService(
            BusinessRepository businessRepository,
            DocumentRepository documentRepository,
            AuditLogService auditLogService,
            FirecrawlClient firecrawlClient,
            CrawlStateWriter crawlStateWriter,
            @Lazy CrawlAsyncRunner crawlAsyncRunner) {
        this.businessRepository = businessRepository;
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
        this.firecrawlClient = firecrawlClient;
        this.crawlStateWriter = crawlStateWriter;
        this.crawlAsyncRunner = crawlAsyncRunner;
    }

    /**
     * Accept a crawl request, persist QUEUED/CRAWLING, and return immediately.
     */
    public CrawlResult startCrawl(UUID businessId) throws BusinessNotFoundException {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + businessId));

        if (business.getCrawlStatus() != null && business.getCrawlStatus().isActive()) {
            return new CrawlResult(false, "Crawl already in progress",
                    business.getCrawlStatus().toPublicStatus(), business);
        }

        if (!firecrawlClient.isConfigured()) {
            crawlStateWriter.markFailed(businessId, "Firecrawl is not configured.");
            auditLogService.logAgentAction(businessId, "Crawler", "CRAWL_FAILED",
                    "{\"limit\":" + MAX_PAGES_TO_CRAWL + "}",
                    "{\"status\":\"FAILED\",\"reason\":\"NOT_CONFIGURED\"}",
                    AgentActionStatus.ERROR, 0L);
            Business failed = businessRepository.findById(businessId).orElse(business);
            return new CrawlResult(false, "Firecrawl is not configured. Set FIRECRAWL_API_KEY in Settings.",
                    CrawlStatus.FAILED, failed);
        }

        crawlStateWriter.markQueued(businessId);
        crawlStateWriter.markCrawling(businessId);
        auditLogService.logAgentAction(businessId, "Crawler", "CRAWL_STARTED",
                "{\"limit\":" + MAX_PAGES_TO_CRAWL + "}",
                "{\"status\":\"CRAWLING\"}",
                AgentActionStatus.SUCCESS, 0L);

        crawlAsyncRunner.runCrawl(businessId);

        Business current = businessRepository.findById(businessId).orElse(business);
        return new CrawlResult(true, "Crawl started. Progress is saved and survives page refresh.",
                CrawlStatus.CRAWLING, current);
    }

    /**
     * Background crawl body. Must not be called from the HTTP thread.
     */
    public void performCrawl(UUID businessId) {
        long start = System.currentTimeMillis();
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) {
            log.warn("Crawl skipped; business {} no longer exists", businessId);
            return;
        }

        try {
            FirecrawlClient.FirecrawlCrawlResponse response = firecrawlClient.executeCrawl(
                    business.getWebsiteUrl(),
                    MAX_PAGES_TO_CRAWL,
                    snapshot -> {
                        int total = snapshot.getTotal() != null ? snapshot.getTotal() : MAX_PAGES_TO_CRAWL;
                        int completed = snapshot.getCompleted() != null ? snapshot.getCompleted() : 0;
                        crawlStateWriter.markProgress(businessId, completed, total);
                    }
            );

            int pagesSaved = 0;
            int pagesSkipped = 0;
            Set<String> existingUrls = getExistingUrls(businessId);

            if (response.getData() != null) {
                for (FirecrawlClient.FirecrawlPageData pageData : response.getData()) {
                    String url = getUrlFromPageData(pageData);
                    String title = getTitleFromPageData(pageData);
                    String content = pageData.getMarkdown();

                    if (existingUrls.contains(url)) {
                        pagesSkipped++;
                        continue;
                    }

                    Document document = new Document();
                    document.setBusiness(business);
                    document.setUrl(url);
                    document.setTitle(title);
                    document.setContent(content);
                    document.setStatus(CrawlStatus.COMPLETED);
                    documentRepository.save(document);
                    pagesSaved++;
                    existingUrls.add(url);
                    crawlStateWriter.markProgress(businessId, pagesSaved, response.getTotal() != null
                            ? response.getTotal() : pagesSaved + pagesSkipped);
                }
            }

            crawlStateWriter.markCompleted(businessId, pagesSaved, pagesSkipped);
            long duration = System.currentTimeMillis() - start;
            auditLogService.logAgentAction(businessId, "Crawler", "CRAWL_COMPLETED",
                    "{\"limit\":" + MAX_PAGES_TO_CRAWL + "}",
                    "{\"status\":\"COMPLETED\",\"pagesSaved\":" + pagesSaved
                            + ",\"pagesSkipped\":" + pagesSkipped + ",\"durationMs\":" + duration + "}",
                    AgentActionStatus.SUCCESS, duration);

        } catch (FirecrawlException e) {
            log.error("Firecrawl error for business {}", businessId, e);
            failOnce(businessId, start, e);
        } catch (Exception e) {
            log.error("Unexpected error during crawl for business {}", businessId, e);
            failOnce(businessId, start, e);
        }
    }

    @Transactional(readOnly = true)
    public Business getBusinessForCrawl(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + businessId));
    }

    public java.util.List<Document> getBusinessDocuments(UUID businessId) {
        return documentRepository.findByBusinessId(businessId);
    }

    private void failOnce(UUID businessId, long start, Throwable error) {
        Business current = businessRepository.findById(businessId).orElse(null);
        if (current != null && current.getCrawlStatus() == CrawlStatus.FAILED) {
            return;
        }
        crawlStateWriter.markFailed(businessId, SafeErrorMessages.CRAWL_FAILED);
        long duration = System.currentTimeMillis() - start;
        auditLogService.logAgentActionWithError(businessId, "Crawler", "CRAWL_FAILED",
                "{\"limit\":" + MAX_PAGES_TO_CRAWL + "}",
                "{\"status\":\"FAILED\",\"durationMs\":" + duration + "}",
                AgentActionStatus.ERROR,
                SafeErrorMessages.CRAWL_FAILED,
                duration);
    }

    private Set<String> getExistingUrls(UUID businessId) {
        Set<String> urls = new HashSet<>();
        documentRepository.findByBusinessId(businessId).forEach(doc -> {
            if (doc.getUrl() != null) {
                urls.add(doc.getUrl());
            }
        });
        return urls;
    }

    private String getUrlFromPageData(FirecrawlClient.FirecrawlPageData pageData) {
        if (pageData.getMetadata() != null && pageData.getMetadata().getSourceURL() != null) {
            return pageData.getMetadata().getSourceURL();
        }
        if (pageData.getUrl() != null) {
            return pageData.getUrl();
        }
        return "";
    }

    private String getTitleFromPageData(FirecrawlClient.FirecrawlPageData pageData) {
        if (pageData.getMetadata() != null && pageData.getMetadata().getTitle() != null) {
            return pageData.getMetadata().getTitle();
        }
        return "Untitled";
    }

    public static class CrawlResult {
        private final boolean success;
        private final String message;
        private final CrawlStatus status;
        private final Business business;

        public CrawlResult(boolean success, String message, CrawlStatus status) {
            this(success, message, status, null);
        }

        public CrawlResult(boolean success, String message, CrawlStatus status, Business business) {
            this.success = success;
            this.message = message;
            this.status = status;
            this.business = business;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public CrawlStatus getStatus() { return status; }
        public Business getBusiness() { return business; }

        public Long getElapsedSeconds() {
            if (business == null || business.getCrawlStartedAt() == null) {
                return null;
            }
            LocalDateTime end = business.getCrawlFinishedAt() != null
                    ? business.getCrawlFinishedAt() : LocalDateTime.now();
            return Math.max(0, Duration.between(business.getCrawlStartedAt(), end).getSeconds());
        }
    }
}
