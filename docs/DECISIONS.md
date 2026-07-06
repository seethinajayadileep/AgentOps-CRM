# Technical Decisions

| Decision ID | Decision | Why | Alternatives Considered | Final Choice | Date |
|-------------|----------|-----|------------------------|--------------|------|
| D-001 | Use monorepo structure | Simplifies coordination between backend and frontend changes, single source of truth, easier for small team | Separate repositories, Polyrepo | Monorepo with backend/, frontend/, docs/, docker/ | 2026-07-01 |
| D-002 | Use Spring Boot 3.x with Java 21 | Java 21 is LTS with modern features, Spring Boot 3.x supports Jakarta EE 9+, large ecosystem | Quarkus, Micronaut, Node.js/Express | Spring Boot 3.2.5 with Java 21 | 2026-07-01 |
| D-003 | Use React + Vite + TypeScript | Vite fast dev server, TypeScript catches errors early, large component ecosystem | Next.js, Vue 3, Svelte | React 18 + Vite 5 + TypeScript 5 | 2026-07-01 |
| D-004 | Use PostgreSQL as primary database | Robust and reliable, pgvector for vectors, full-text search | MySQL, MongoDB | PostgreSQL 16 | 2026-07-01 |
| D-005 | Use Redis for cache and queue | Fast in-memory storage, pub/sub messaging, excellent Spring integration | RabbitMQ, Apache Kafka, Hazelcast | Redis 7 | 2026-07-01 |
| D-006 | Start with REST API | Simpler to implement, easier to debug, can evolve to GraphQL later | GraphQL only | REST API (defer GraphQL decision) | 2026-07-01 |
| D-007 | Use Flyway for migrations | Native Spring Boot integration, version-controlled schema changes | Liquibase | Flyway 10.20.1 | 2026-07-01 |
| D-008 | Defer vector database choice to Phase 3 | Need to evaluate scale and performance requirements first | pgvector now, Qdrant now | Decision pending (evaluate both in Phase 3) | 2026-07-01 |
| D-009 | Defer AI framework choice to Phase 3 | Spring AI newer with better integration, LangChain4j more mature | Spring AI now, LangChain4j now | Decision pending (evaluate both in Phase 3) | 2026-07-01 |
| D-010 | Use WebSocket for chat | Bidirectional communication, lower latency, better UX | Server-Sent Events, Long polling | WebSocket | 2026-07-01 |
| D-011 | Use Tailwind CSS for styling | Utility-first, no custom CSS, good dark mode, small bundle size | CSS Modules, styled-components, Material UI | Tailwind CSS 3.4.3 | 2026-07-01 |
| D-012 | Remove Lombok (B-003) | Lombok annotation processing not working with Java 21 on this system | Fix Lombok config, use records | Plain Java getters/setters | 2026-07-01 |
| D-013 | Temporarily disable Flyway (B-004) | Connection timing issues with PostgreSQL during startup | Add health check delay, retry mechanism | Disable for Phase 0, re-enable in Phase 1 | 2026-07-01 |
| D-014 | Use UUID for primary keys | Better for distributed systems, no ID guessing, more secure than sequential IDs | Long (auto-increment), String IDs | UUID with GenerationType.UUID | 2026-07-01 |
| D-015 | Use YAML configuration instead of properties | More readable, supports complex structures, better for environments | .properties files, environment variables only | application.yml with profiles | 2026-07-01 |
| D-016 | Use BaseEntity and AuditableEntity pattern | Reduces code duplication, ensures consistent timestamp handling across all entities | No base classes, copy-paste getters/setters, AOP | Abstract base classes with inheritance | 2026-07-01 |
| D-017 | Use separate Enum classes for type safety | Better than String constants, compile-time checking, cleaner code | String constants, database-only enums | Dedicated enum classes in entity.enums package | 2026-07-01 |
| D-018 | Use cascade-delete for relationships | Maintains data integrity when parent is deleted, simplifies cleanup logic | Manual cleanup, soft deletes | JPA cascade with orphanRemoval where appropriate | 2026-07-01 |
| D-019 | Use LAZY fetching for collections | Better performance, only load data when needed, reduces memory usage | EAGER fetching, manual queries | FetchType.LAZY for @ManyToOne and collections | 2026-07-01 |
| D-020 | Use PostgreSQL JSONB for flexible metadata | Allows storing variable data without schema changes, flexible for different content types | Separate columns, TEXT only, NoSQL database | JSONB column with validation in application | 2026-07-01 |
| D-021 | Use Firecrawl adapter pattern for website crawling | Abstracts Firecrawl-specific logic, allows easy replacement, cleaner testing | Direct HTTP calls, separate microservice | Adapter pattern with dedicated client class | 2026-07-01 |
| D-022 | Store crawled content as markdown in TEXT column | Simple storage, good for RAG, easy to search with PostgreSQL full-text | HTML only, multiple format columns, separate storage | Markdown in Document.content (TEXT) | 2026-07-01 |
| D-023 | Interim vector storage as TEXT + in-memory cosine (F-004) | Unblocks RAG without changing the DB image; current `postgres:16-alpine` lacks the pgvector extension. Business isolation is enforced at the repository layer (`findByBusinessId`) BEFORE ranking, so correctness/security is unaffected by the ranking backend | Full pgvector now (needs `pgvector/pgvector:pg16` image + volume recreate), Qdrant (no existing config, rejected per requirement) | `rag.vector-store=postgres-text`: embedding stored as pgvector-style string in `knowledge_chunks.embedding`, cosine similarity computed in `VectorStoreService`. Planned upgrade to pgvector (V3 migration) resolves D-004/D-008 | 2026-07-02 |
| D-024 | Use OpenAI `text-embedding-3-small` (1536-dim) for embeddings | Cheap, high-quality, widely supported; batched requests | `text-embedding-3-large`, local/open models | OpenAI `text-embedding-3-small`, key via `OPENAI_API_KEY` env var | 2026-07-02 |
| D-025 | Source Firecrawl API key from env var, remove hardcoded key | Security: secrets must not live in `application.yml`; the previously committed key must be rotated | Keep key in yml, encrypted config | `firecrawl.api-key=${FIRECRAWL_API_KEY:}` (matches OpenAI pattern) | 2026-07-02 |
| D-026 | Isolate all Apify actor input/output inside `ApifyClient` with a normalized `ApifyLeadResult` (F-010) | Apify actor schemas vary widely and actors may be swapped; keeping the rest of the system dependent only on a stable normalized shape means changing actors never ripples into services/controllers/UI | Parse raw Apify JSON directly in the service, one service per actor | Adapter/client pattern (mirrors Firecrawl D-021); broad multi-key actor input builder + tolerant field-name normalizer | 2026-07-03 |
| D-027 | Evaluation Agent fails CLOSED with a deterministic rule-based fallback (F-008 / F-026) | The Evaluation Agent is a customer-facing safety gate; if the LLM is unconfigured, times out, or returns malformed JSON we must never silently ship an ungrounded answer. A keyword-based rule engine (pricing/discount/guarantee/legal groups cross-checked against retrieved chunks) gives a safe verdict without the LLM, and total failure returns the safe fallback answer. | Fail open (send the draft when evaluation errors), block all answers on any error, retry LLM only | LLM-first with rule-based fallback; on total failure return unsafe + safe fallback; every path audited in AgentLog (EVALUATION_STARTED/COMPLETED/FAILED, UNSAFE_ANSWER_BLOCKED, FALLBACK_ANSWER_USED, FALLBACK_USED) | 2026-07-03 |
| D-028 | Do not evaluate greetings or the plain "no info" fallback; keep evaluation additive on the chat response (F-008 / F-026) | Greetings and the existing no-context fallback are already safe and not grounded in chunks, so running them through the grounding check would wrongly replace friendly greetings with the fallback. Exposing evaluation as an additive nested object (only `hallucinationRisk`/`safeToSend`/`reason`) preserves the F-006 `leadDetected`/`leadId` contract and never leaks the internal prompt. | Evaluate every message, replace the whole response shape, expose full evaluation incl. internal reason/prompt | Skip greeting + no-info fallback; additive `evaluation` object with customer-safe fields only | 2026-07-03 |
| D-027 | Apify Lead Finder is optional and degrades gracefully | The app must always start even without Apify configured; outbound discovery is a value-add, not a hard dependency | Fail startup if token missing, hide feature entirely | `apify.enabled` + token check in `ApifyClient.isConfigured()`; endpoints return 503 + `"Apify is not configured."`; UI shows a banner | 2026-07-03 |
| D-028 | Reuse the existing `Lead` table for imported prospects; require a `targetBusinessId` on import | Avoids a parallel prospect model and keeps one funnel; `Lead.business` is non-null so a business must be chosen. Auto-selects when only one business exists | Separate `Prospect` table, make `Lead.business` nullable | Import maps DiscoveredLead → Lead (name = contactName || businessName, requirement = "Outbound prospect discovered via Apify"), source recorded in summary; dedupe by email→phone→website→name+location | 2026-07-03 |

## Firecrawl API Integration Notes (D-021)

Based on [Firecrawl Crawl API Reference](https://docs.firecrawl.dev/api-reference/crawl):

**Request Format (v2):**
```json
POST https://api.firecrawl.dev/v1/crawl
Headers: Authorization: Bearer {API_KEY}

{
  "url": "https://example.com",
  "crawlerOptions": {
    "limit": 30
  },
  "scrapeOptions": {
    "formats": ["markdown"],
    "onlyMainContent": true
  }
}
```

**Response Format:**
- Processing (HTTP 202): `{success: true, id: "...", status: "processing"}`
- Completed (HTTP 200): `{success: true, status: "completed", total: 5, data: [{markdown: "...", metadata: {title, sourceURL, statusCode}, completedAt: "..."}]}`

## Vector Database Decision (D-008)

Based on [pgvector documentation](https://github.com/pgvector/pgvector):

**Choice: pgvector**

**Why:**
- Native PostgreSQL extension, no separate infrastructure needed
- Good performance for small to medium datasets (100K-1M chunks)
- No additional service to manage
- Cosine similarity search with `<=>` operator
- HNSW and IVFFlat indexing for performance

**Implementation:**
- Vectors stored as array strings: `'[0.1, 0.2, 0.3]'`
- Use native SQL queries with `<=>` for similarity search
- Create HNSW index for performance
- Maximum 2000 dimensions (OpenAI embeddings are 1536, fits well)

**Migration:**
```sql
CREATE EXTENSION IF NOT EXISTS vector;
ALTER TABLE knowledge_chunks ADD COLUMN embedding vector(1536);
CREATE INDEX ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
```

**Vector Search Query:**
```sql
SELECT * FROM knowledge_chunks
WHERE business_id = :businessId
ORDER BY embedding <=> :queryVector::vector
LIMIT 5;
```

**Source:** [pgvector GitHub](https://github.com/pgvector/pgvector)

## RAG Implementation Notes (F-004)

**Chunking Strategy:**
- Fixed size chunks: ~1000 characters (approx 150-200 tokens)
- Overlap: 200 characters between chunks to maintain context
- Preserve sentence boundaries where possible
- Skip chunks < 50 characters

**Embedding Service:**
- Provider: OpenAI text-embedding-3-small (1536 dimensions, fast & cost-effective)
- Fallback: Can be configured to other providers
- Environment variable: `OPENAI_API_KEY`
- Store as pgvector array string: `'[0.1, 0.2, ...]'`

**RAG Search Flow:**
1. Receive user query
2. Generate query embedding
3. Vector similarity search (filtered by business ID - critical for security)
4. Return top 5 chunks with source URLs
5. Format results for frontend

**Security:**
- Always filter by business_id in search (no cross-business leakage)
- Validate business exists before processing
- Log all RAG operations via AgentLog

**Template for New Decisions**