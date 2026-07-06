-- AgentOps CRM - Database Schema V7
-- F-010: Apify Lead Finder
-- Creates tables for outbound lead discovery runs and the prospects they discover.
--
-- Note: Flyway is currently disabled (spring.flyway.enabled=false) and Hibernate ddl-auto=update
-- auto-creates these tables. This migration is kept for documentation and future enablement.

-- Lead source runs (one per Apify actor search execution)
CREATE TABLE IF NOT EXISTS lead_source_runs (
    id UUID PRIMARY KEY,
    search_name VARCHAR(255),
    industry VARCHAR(255),
    location VARCHAR(255),
    keywords VARCHAR(500),
    actor_id VARCHAR(255),
    apify_run_id VARCHAR(255),
    apify_dataset_id VARCHAR(255),
    max_results INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_results INTEGER NOT NULL DEFAULT 0,
    imported_count INTEGER NOT NULL DEFAULT 0,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_lead_source_runs_status ON lead_source_runs(status);
CREATE INDEX IF NOT EXISTS idx_lead_source_runs_apify_run_id ON lead_source_runs(apify_run_id);
CREATE INDEX IF NOT EXISTS idx_lead_source_runs_created_at ON lead_source_runs(created_at);

-- Discovered leads (normalized prospects for admin review)
CREATE TABLE IF NOT EXISTS discovered_leads (
    id UUID PRIMARY KEY,
    lead_source_run_id UUID NOT NULL,
    business_name VARCHAR(500),
    website_url VARCHAR(1000),
    contact_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    location VARCHAR(255),
    industry VARCHAR(255),
    source_url VARCHAR(1000),
    raw_data_json TEXT,
    score DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    imported_lead_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_discovered_leads_run FOREIGN KEY (lead_source_run_id)
        REFERENCES lead_source_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_discovered_leads_run_id ON discovered_leads(lead_source_run_id);
CREATE INDEX IF NOT EXISTS idx_discovered_leads_status ON discovered_leads(status);
CREATE INDEX IF NOT EXISTS idx_discovered_leads_email ON discovered_leads(email);
CREATE INDEX IF NOT EXISTS idx_discovered_leads_phone ON discovered_leads(phone);
CREATE INDEX IF NOT EXISTS idx_discovered_leads_website_url ON discovered_leads(website_url);
