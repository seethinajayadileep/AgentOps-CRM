-- Add style column to approvals table for follow-up message types
-- F-007: Follow-up Approval System
ALTER TABLE approvals ADD COLUMN IF NOT EXISTS style VARCHAR(50);
