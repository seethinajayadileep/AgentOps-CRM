package com.agentopscrm.service;

import com.agentopscrm.client.FirecrawlClient;
import com.agentopscrm.client.FirecrawlClient.FirecrawlException;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Document;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for website crawling operations.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private static final int MAX_PAGES_TO_CRAWL = 30;

    private final BusinessRepository businessRepository;
    private final DocumentRepository documentRepository;
    private final AgentLogRepository agentLogRepository;
    private final FirecrawlClient firecrawlClient;

    public CrawlService(
            BusinessRepository businessRepository,
            DocumentRepository documentRepository,
            AgentLogRepository agentLogRepository,
            FirecrawlClient firecrawlClient) {
        this.businessRepository = businessRepository;
        this.documentRepository = documentRepository;
        this.agentLogRepository = agentLogRepository;
        this.firecrawlClient = firecrawlClient;
    }

    /**
     * Start crawling a business's website.
     *
     * @param businessId The business ID
     * @return Crawl result with status and message
     * @throws BusinessNotFoundException if business not found
     */
    @Transactional
    public CrawlResult startCrawl(UUID businessId) throws BusinessNotFoundException {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + businessId));

        // Check if already crawling
        if (business.getCrawlStatus() == CrawlStatus.IN_PROGRESS) {
            return new CrawlResult(false, "Crawl already in progress", CrawlStatus.IN_PROGRESS);
        }

        // Check if Firecrawl is configured
        if (!firecrawlClient.isConfigured()) {
            business.setCrawlStatus(CrawlStatus.FAILED);
            businessRepository.save(business);
            logAgentAction("Crawler", "START_CRAWL", businessId.toString(),
                    "{\"websiteUrl\":\"" + business.getWebsiteUrl() + "\"}",
                    "{\"error\":\"FIRECRAWL_API_KEY not configured\"}",
                    AgentActionStatus.ERROR);
            return new CrawlResult(false, "Firecrawl API key not configured. Please set FIRECRAWL_API_KEY.", CrawlStatus.FAILED);
        }

        // Set status to crawling
        business.setCrawlStatus(CrawlStatus.IN_PROGRESS);
        businessRepository.save(business);

        logAgentAction("Crawler", "START_CRAWL", businessId.toString(),
                "{\"websiteUrl\":\"" + business.getWebsiteUrl() + "\",\"limit\":" + MAX_PAGES_TO_CRAWL + "}",
                "{\"status\":\"started\"}",
                AgentActionStatus.SUCCESS);

        try {
            // Execute crawl
            FirecrawlClient.FirecrawlCrawlResponse response = firecrawlClient.executeCrawl(
                    business.getWebsiteUrl(),
                    MAX_PAGES_TO_CRAWL
            );

            int pagesSaved = 0;
            int pagesSkipped = 0;
            Set<String> existingUrls = getExistingUrls(businessId);

            if (response.getData() != null) {
                for (FirecrawlClient.FirecrawlPageData pageData : response.getData()) {
                    String url = getUrlFromPageData(pageData);
                    String title = getTitleFromPageData(pageData);
                    String content = pageData.getMarkdown();

                    // Skip if URL already exists
                    if (existingUrls.contains(url)) {
                        pagesSkipped++;
                        continue;
                    }

                    // Save document
                    Document document = new Document();
                    document.setBusiness(business);
                    document.setUrl(url);
                    document.setTitle(title);
                    document.setContent(content);
                    document.setStatus(CrawlStatus.COMPLETED);

                    documentRepository.save(document);
                    pagesSaved++;
                    existingUrls.add(url);

                    // Log each page saved (sample - don't log every page if many)
                    if (pagesSaved <= 5 || pagesSaved % 10 == 0) {
                        log.info("Saved page {}/{}: {}", pagesSaved, response.getTotal(), url);
                    }
                }
            }

            // Update business status
            business.setCrawlStatus(CrawlStatus.COMPLETED);
            businessRepository.save(business);

            // Log completion
            logAgentAction("Crawler", "CRAWL_COMPLETED", businessId.toString(),
                    "{\"websiteUrl\":\"" + business.getWebsiteUrl() + "\"}",
                    "{\"pagesSaved\":" + pagesSaved + ",\"pagesSkipped\":" + pagesSkipped + "}",
                    AgentActionStatus.SUCCESS);

            String message = String.format("Crawl completed. %d pages saved, %d duplicates skipped.",
                    pagesSaved, pagesSkipped);
            return new CrawlResult(true, message, CrawlStatus.COMPLETED);

        } catch (FirecrawlException e) {
            log.error("Firecrawl error for business {}", businessId, e);

            // Update business status to failed
            business.setCrawlStatus(CrawlStatus.FAILED);
            businessRepository.save(business);

            // Log failure
            logAgentAction("Crawler", "CRAWL_FAILED", businessId.toString(),
                    "{\"websiteUrl\":\"" + business.getWebsiteUrl() + "\"}",
                    "{\"error\":\"" + e.getMessage() + "\"}",
                    AgentActionStatus.ERROR);

            return new CrawlResult(false, "Crawl failed: " + e.getMessage(), CrawlStatus.FAILED);

        } catch (Exception e) {
            log.error("Unexpected error during crawl for business {}", businessId, e);

            // Update business status to failed
            business.setCrawlStatus(CrawlStatus.FAILED);
            businessRepository.save(business);

            // Log failure
            logAgentAction("Crawler", "CRAWL_FAILED", businessId.toString(),
                    "{\"websiteUrl\":\"" + business.getWebsiteUrl() + "\"}",
                    "{\"error\":\"" + e.getMessage() + "\"}",
                    AgentActionStatus.ERROR);

            return new CrawlResult(false, "Unexpected error: " + e.getMessage(), CrawlStatus.FAILED);
        }
    }

    /**
     * Get all documents for a business.
     */
    public java.util.List<Document> getBusinessDocuments(UUID businessId) {
        return documentRepository.findByBusinessId(businessId);
    }

    /**
     * Get existing URLs for a business (for deduplication).
     */
    private Set<String> getExistingUrls(UUID businessId) {
        Set<String> urls = new HashSet<>();
        documentRepository.findByBusinessId(businessId).forEach(doc -> {
            if (doc.getUrl() != null) {
                urls.add(doc.getUrl());
            }
        });
        return urls;
    }

    /**
     * Extract URL from page data.
     */
    private String getUrlFromPageData(FirecrawlClient.FirecrawlPageData pageData) {
        if (pageData.getMetadata() != null && pageData.getMetadata().getSourceURL() != null) {
            return pageData.getMetadata().getSourceURL();
        }
        if (pageData.getUrl() != null) {
            return pageData.getUrl();
        }
        return "";
    }

    /**
     * Extract title from page data.
     */
    private String getTitleFromPageData(FirecrawlClient.FirecrawlPageData pageData) {
        if (pageData.getMetadata() != null && pageData.getMetadata().getTitle() != null) {
            return pageData.getMetadata().getTitle();
        }
        return "Untitled";
    }

    /**
     * Log an agent action.
     */
    private void logAgentAction(String agentName, String action, String businessId, String inputJson,
                                 String outputJson, AgentActionStatus status) {
        try {
            AgentLog logEntry = new AgentLog();
            logEntry.setAgentName(agentName);
            logEntry.setAction(action);
            logEntry.setInputJson(inputJson);
            logEntry.setOutputJson(outputJson);
            logEntry.setStatus(status);

            Business business = businessRepository.findById(UUID.fromString(businessId)).orElse(null);
            logEntry.setBusiness(business);

            agentLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log agent action", e);
        }
    }

    /**
     * Result of a crawl operation.
     */
    public static class CrawlResult {
        private final boolean success;
        private final String message;
        private final CrawlStatus status;

        public CrawlResult(boolean success, String message, CrawlStatus status) {
            this.success = success;
            this.message = message;
            this.status = status;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public CrawlStatus getStatus() { return status; }
    }
}