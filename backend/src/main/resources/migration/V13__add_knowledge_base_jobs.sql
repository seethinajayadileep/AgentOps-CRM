-- Bug 2 fix: Asynchronous knowledge-base build job workflow.
-- Converts the synchronous crawl/build request into a background job so the
-- frontend can poll for real progress instead of experiencing a client/proxy
-- timeout while the backend continues processing.

CREATE TABLE IF NOT EXISTS knowledge_base_jobs (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    documents_total INTEGER NOT NULL DEFAULT 0,
    documents_processed INTEGER NOT NULL DEFAULT 0,
    chunks_created INTEGER NOT NULL DEFAULT 0,
    embeddings_created INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_kb_jobs_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_kb_jobs_business_id ON knowledge_base_jobs(business_id);
CREATE INDEX IF NOT EXISTS idx_kb_jobs_status ON knowledge_base_jobs(status);
CREATE INDEX IF NOT EXISTS idx_kb_jobs_started_at ON knowledge_base_jobs(started_at);
