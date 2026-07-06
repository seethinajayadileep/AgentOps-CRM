-- Migration: Add index on conversations.updated_at for efficient sorting
-- Version: V9
-- Description: Optimize conversation list queries sorted by updated_at
-- Author: AgentOps Team
-- Date: 2026-07-05

-- Add index on updated_at for efficient ORDER BY in list queries
CREATE INDEX IF NOT EXISTS idx_conversations_updated_at ON conversations(updated_at DESC);

-- The index helps with queries like:
-- SELECT * FROM conversations ORDER BY updated_at DESC, created_at DESC;
