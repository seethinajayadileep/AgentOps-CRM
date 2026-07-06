package com.agentopscrm.entity.enums;

/**
 * Status of an asynchronous knowledge-base build job (Bug 2 fix).
 *
 * @author AgentOps Team
 * @version 0.1.0
 */
public enum KnowledgeBaseJobStatus {
    /** Job accepted and queued, not yet started. */
    QUEUED,
    /** Crawling source documents (reserved for future crawl-integrated jobs). */
    CRAWLING,
    /** Splitting documents into chunks. */
    CHUNKING,
    /** Generating and storing embeddings for chunks. */
    EMBEDDING,
    /** Job finished successfully. */
    COMPLETED,
    /** Job finished but some documents/chunks failed (partial success). */
    PARTIAL,
    /** Job failed. */
    FAILED;

    /**
     * @return true if this status represents a job that is still in progress
     *         (used for duplicate-build prevention and restore-on-refresh).
     */
    public boolean isActive() {
        return this == QUEUED || this == CRAWLING || this == CHUNKING || this == EMBEDDING;
    }

    /**
     * @return true if this status is a terminal state.
     */
    public boolean isTerminal() {
        return !isActive();
    }
}
