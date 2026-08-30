package com.agentopscrm.service;

import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.util.SafeErrorMessages;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Commits crawl status updates in independent transactions so a long-running
 * Firecrawl poll cannot hide QUEUED/CRAWLING from the UI.
 */
@Component
public class CrawlStateWriter {

    private final BusinessRepository businessRepository;

    public CrawlStateWriter(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Business markQueued(UUID businessId) {
        Business business = businessRepository.findById(businessId).orElseThrow();
        business.setCrawlStatus(CrawlStatus.QUEUED);
        business.setCrawlStartedAt(LocalDateTime.now());
        business.setCrawlFinishedAt(null);
        business.setCrawlError(null);
        business.setCrawlPagesSaved(0);
        business.setCrawlPagesTotal(0);
        business.setUpdatedAt(LocalDateTime.now());
        return businessRepository.saveAndFlush(business);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Business markCrawling(UUID businessId) {
        Business business = businessRepository.findById(businessId).orElseThrow();
        business.setCrawlStatus(CrawlStatus.CRAWLING);
        if (business.getCrawlStartedAt() == null) {
            business.setCrawlStartedAt(LocalDateTime.now());
        }
        business.setUpdatedAt(LocalDateTime.now());
        return businessRepository.saveAndFlush(business);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProgress(UUID businessId, int pagesSaved, int pagesTotal) {
        businessRepository.findById(businessId).ifPresent(business -> {
            if (!business.getCrawlStatus().isActive()) {
                return;
            }
            business.setCrawlStatus(CrawlStatus.CRAWLING);
            business.setCrawlPagesSaved(pagesSaved);
            business.setCrawlPagesTotal(Math.max(pagesTotal, pagesSaved));
            business.setUpdatedAt(LocalDateTime.now());
            businessRepository.saveAndFlush(business);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID businessId, int pagesSaved, int pagesSkipped) {
        businessRepository.findById(businessId).ifPresent(business -> {
            if (business.getCrawlStatus() == CrawlStatus.COMPLETED
                    || business.getCrawlStatus() == CrawlStatus.FAILED) {
                return;
            }
            business.setCrawlStatus(CrawlStatus.COMPLETED);
            business.setCrawlPagesSaved(pagesSaved);
            business.setCrawlPagesTotal(pagesSaved + pagesSkipped);
            business.setCrawlError(null);
            business.setCrawlFinishedAt(LocalDateTime.now());
            business.setUpdatedAt(LocalDateTime.now());
            businessRepository.saveAndFlush(business);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID businessId, String safeError) {
        businessRepository.findById(businessId).ifPresent(business -> {
            if (business.getCrawlStatus() == CrawlStatus.COMPLETED
                    || business.getCrawlStatus() == CrawlStatus.FAILED) {
                return;
            }
            business.setCrawlStatus(CrawlStatus.FAILED);
            business.setCrawlError(safeError != null ? safeError : SafeErrorMessages.CRAWL_FAILED);
            business.setCrawlFinishedAt(LocalDateTime.now());
            business.setUpdatedAt(LocalDateTime.now());
            businessRepository.saveAndFlush(business);
        });
    }
}
