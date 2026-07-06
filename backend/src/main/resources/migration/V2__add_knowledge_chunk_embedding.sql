-- AgentOps CRM - Database Schema V2
-- Feature: F-004 RAG Knowledge Base
-- Adds vector storage + chunk ordering to knowledge_chunks.
--
-- NOTE: This uses a TEXT column to persist embeddings without requiring the
-- pgvector extension. Similarity search is computed in-memory over
-- business-scoped chunks (see RagService / VectorStoreService).
-- A future migration (V3) will introduce a native pgvector column once the
-- database image ships the `vector` extension. See docs/DECISIONS.md (D-004).

-- Serialized embedding vector, pgvector-style string e.g. "[0.12,0.34,...]"
ALTER TABLE knowledge_chunks
    ADD COLUMN IF NOT EXISTS embedding TEXT;

-- 0-based position of the chunk within its source document
ALTER TABLE knowledge_chunks
    ADD COLUMN IF NOT EXISTS chunk_index INTEGER;
