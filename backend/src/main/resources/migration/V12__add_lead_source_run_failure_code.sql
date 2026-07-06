-- Bug 4 fix: Apify Lead Finder run reconciliation and safe failure classification.
-- Adds a machine-readable failure_code (e.g. APIFY_UNAUTHORIZED) distinct from the
-- human-readable failure_reason, plus a last_synced_at timestamp used to detect and
-- recover stale RUNNING runs.

ALTER TABLE lead_source_runs ADD COLUMN failure_code VARCHAR(50);
ALTER TABLE lead_source_runs ADD COLUMN last_synced_at TIMESTAMP;
