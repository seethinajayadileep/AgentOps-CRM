package com.agentopscrm.entity.enums;

/**
 * Status of an asynchronous knowledge-base build job.
 */
public enum KnowledgeBaseJobStatus {
    /** Job accepted and queued — preparing. */
    QUEUED,
    /** Reserved for crawl-integrated builds. */
    CRAWLING,
    /** Splitting documents into chunks. */
    CHUNKING,
    /** Generating and storing embeddings. */
    EMBEDDING,
    /** Writing vectors / finalizing the index. */
    INDEXING,
    /** Job finished successfully. */
    COMPLETED,
    /** Job finished but some documents/chunks failed. */
    PARTIAL,
    /** Job failed. */
    FAILED;

    public boolean isActive() {
        return this == QUEUED || this == CRAWLING || this == CHUNKING
                || this == EMBEDDING || this == INDEXING;
    }

    public boolean isTerminal() {
        return !isActive();
    }
}
