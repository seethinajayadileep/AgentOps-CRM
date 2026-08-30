package com.agentopscrm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Runs website crawls on a background thread so POST /crawl can commit
 * QUEUED/CRAWLING immediately.
 */
@Component
public class CrawlAsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(CrawlAsyncRunner.class);

    private final CrawlService crawlService;

    public CrawlAsyncRunner(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @Async
    public void runCrawl(UUID businessId) {
        try {
            crawlService.performCrawl(businessId);
        } catch (Exception e) {
            log.error("Async crawl failed for business {}", businessId, e);
        }
    }
}
