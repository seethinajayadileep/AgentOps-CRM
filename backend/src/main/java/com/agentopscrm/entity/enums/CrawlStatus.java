package com.agentopscrm.entity.enums;

/**
 * Status of website crawling for a business.
 *
 * {@link #IN_PROGRESS} is retained as a persisted alias of {@link #CRAWLING}
 * for rows written before the queued/crawling split.
 */
public enum CrawlStatus {
    NOT_STARTED,
    QUEUED,
    CRAWLING,
    IN_PROGRESS,
    COMPLETED,
    FAILED;

    public boolean isActive() {
        return this == QUEUED || this == CRAWLING || this == IN_PROGRESS;
    }

    /**
     * Public status shown to operators: collapse the legacy alias into CRAWLING.
     */
    public CrawlStatus toPublicStatus() {
        return this == IN_PROGRESS ? CRAWLING : this;
    }
}
