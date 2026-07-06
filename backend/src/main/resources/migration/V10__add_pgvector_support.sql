-- Migration: Add pgvector extension and embedding_vector column
-- Version: V10
-- Description: Enable pgvector extension and add vector column for native vector similarity search
-- Author: AgentOps Team
-- Date: 2026-07-05
-- Flyway Config: executeInTransaction=false (required for CREATE INDEX CONCURRENTLY)

-- Enable the pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding_vector column to knowledge_chunks table
-- Using vector(1536) for OpenAI text-embedding-3-small dimension
ALTER TABLE knowledge_chunks
ADD COLUMN IF NOT EXISTS embedding_vector vector(1536);

-- Migrate existing TEXT embeddings to vector column safely
-- Only migrate valid pgvector format strings: "[num,num,...]"
-- Skip NULL, empty, or invalid values
DO $$
BEGIN
    UPDATE knowledge_chunks
    SET embedding_vector = CAST(embedding AS vector)
    WHERE embedding IS NOT NULL
      AND embedding != ''
      AND embedding != '[]'
      AND embedding LIKE '[%]'
      AND embedding_vector IS NULL;
    
    RAISE NOTICE 'Migrated % existing embeddings to pgvector format', 
        (SELECT COUNT(*) FROM knowledge_chunks WHERE embedding_vector IS NOT NULL);
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Warning: Some embeddings could not be migrated: %', SQLERRM;
        -- Continue migration even if some rows fail
END $$;

-- Add index for efficient vector similarity search
-- Using ivfflat index for approximate nearest neighbor search
-- Lists parameter set to a reasonable value (rows/1000, minimum 10)
-- This index will be built asynchronously and won't block the table
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_knowledge_chunks_embedding_vector
ON knowledge_chunks USING ivfflat (embedding_vector vector_cosine_ops)
WITH (lists = 100);

-- Note: The existing 'embedding' TEXT column is preserved for rollback compatibility
-- New embeddings will be stored in both columns temporarily
-- The TEXT column can be removed in a future migration once pgvector is fully validated
