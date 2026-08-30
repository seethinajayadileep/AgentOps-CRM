-- Persist long-running website crawl progress so the UI can poll after refresh
-- and a frontend timeout cannot imply the backend job failed.

ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS crawl_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS crawl_finished_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS crawl_error TEXT,
    ADD COLUMN IF NOT EXISTS crawl_pages_saved INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS crawl_pages_total INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_businesses_crawl_started_at ON businesses(crawl_started_at);
