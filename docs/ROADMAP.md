# Roadmap

## Phase 0: Foundation ✅

**Duration**: 1 day
**Status**: DONE
**Completion Date**: 2026-07-01

### Objectives
- Set up monorepo structure
- Create backend skeleton
- Create frontend skeleton
- Set up infrastructure (Docker)
- Create documentation framework

### Tasks Completed
- ✅ Monorepo structure with backend/, frontend/, docs/, docker/
- ✅ Spring Boot 3.2.5 backend with Java 21
- ✅ React + Vite + TypeScript frontend
- ✅ Health endpoint implementation (API-001)
- ✅ Docker Compose for PostgreSQL and Redis
- ✅ All documentation files created
- ✅ 3 bugs fixed (B-001, B-003, B-004)

### Next Phase
→ Phase 1: Business Management

---

## Phase 1: Business Management

**Duration**: 3-4 days
**Status**: NOT_STARTED

### Objectives
- Database schema design
- Business CRUD operations
- Admin UI for business management
- Data validation

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-001 | Design database schema (Business, Lead, Conversation entities) | HIGH | NOT_STARTED |
| F-002 | Create Flyway migrations for all tables | HIGH | NOT_STARTED |
| F-003 | Implement Business entity with JPA | HIGH | NOT_STARTED |
| F-004 | Create BusinessRepository | HIGH | NOT_STARTED |
| F-005 | Implement BusinessService | HIGH | NOT_STARTED |
| F-006 | Create BusinessController with CRUD endpoints | HIGH | NOT_STARTED |
| F-007 | Add validation to DTOs | MEDIUM | NOT_STARTED |
| F-008 | Implement Businesses frontend page | MEDIUM | NOT_STARTED |
| F-009 | Add business creation form | MEDIUM | NOT_STARTED |
| F-010 | Add business list with pagination | MEDIUM | NOT_STARTED |

### Deliverables
- Database schema with migrations
- Business CRUD API (API-003 to API-007)
- Business management UI

### Dependencies
- PostgreSQL must be running
- Flyway must be enabled

---

## Phase 2: Website Crawling

**Duration**: 3-4 days
**Status**: NOT_STARTED

### Objectives
- Integrate Firecrawl for website crawling
- Store crawled pages in database
- Process and clean content

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-007 | Integrate Firecrawl API client | HIGH | NOT_STARTED |
| F-008 | Create CrawlJob entity and repository | HIGH | NOT_STARTED |
| F-009 | Create Page entity for storing crawled content | HIGH | NOT_STARTED |
| F-010 | Implement CrawlService with Firecrawl | HIGH | NOT_STARTED |
| F-011 | Create crawl job queue with Redis | MEDIUM | NOT_STARTED |
| F-012 | Implement crawl status tracking | MEDIUM | NOT_STARTED |
| F-013 | Add crawling UI to Businesses page | MEDIUM | NOT_STARTED |
| F-014 | Display crawl progress | LOW | NOT_STARTED |

### Deliverables
- Crawl job management
- Page storage in database
- Crawling UI (API-008 to API-010)

### Dependencies
- Phase 1 completed (Business entity exists)

---

## Phase 3: RAG (Retrieval-Augmented Generation)

**Duration**: 5-7 days
**Status**: NOT_STARTED

### Objectives
- Set up vector database
- Generate embeddings
- Build semantic search
- Implement RAG query engine

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| D-008 | Choose vector database (pgvector vs Qdrant) | HIGH | NOT_STARTED |
| F-010 | Set up vector database | HIGH | NOT_STARTED |
| D-009 | Choose AI framework (Spring AI vs LangChain4j) | HIGH | NOT_STARTED |
| F-011 | Implement embedding service | HIGH | NOT_STARTED |
| F-012 | Create content chunking strategy | HIGH | NOT_STARTED |
| F-013 | Store vectors in database | HIGH | NOT_STARTED |
| F-014 | Implement semantic search | HIGH | NOT_STARTED |
| F-015 | Build RAG query pipeline | HIGH | NOT_STARTED |
| F-016 | Format AI responses | MEDIUM | NOT_STARTED |
| F-017 | Store prompts in resources/prompts/ | MEDIUM | NOT_STARTED |

### Deliverables
- Vector database setup
- Embedding generation
- Semantic search API
- RAG query engine

### Dependencies
- Phase 2 completed (pages exist to embed)

---

## Phase 4: Chat Agent

**Duration**: 4-5 days
**Status**: NOT_STARTED

### Objectives
- Real-time chat with customers
- AI-powered responses using RAG
- Conversation tracking

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-015 | Create ChatWidget React component | HIGH | NOT_STARTED |
| F-016 | Implement WebSocket for real-time chat | HIGH | NOT_STARTED |
| F-017 | Create Conversation and Message entities | HIGH | NOT_STARTED |
| F-018 | Implement ChatController with WebSocket | HIGH | NOT_STARTED |
| F-019 | Integrate RAG for chat responses | HIGH | NOT_STARTED |
| F-020 | Implement conversation storage | MEDIUM | NOT_STARTED |
| F-021 | Add context management | MEDIUM | NOT_STARTED |
| F-022 | Create Conversations admin UI | MEDIUM | NOT_STARTED |
| F-023 | Display conversation history | MEDIUM | NOT_STARTED |

### Deliverables
- Real-time chat widget
- WebSocket chat API (API-011 to API-013)
- Conversations admin UI

### Dependencies
- Phase 3 completed (RAG query engine available)

---

## Phase 5: Lead Qualification

**Duration**: 3-4 days
**Status**: NOT_STARTED

### Objectives
- Identify qualified leads from conversations
- Lead scoring algorithm
- Lead extraction from chat

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-018 | Define lead qualification criteria | HIGH | NOT_STARTED |
| F-019 | Create Lead entity with scoring | HIGH | NOT_STARTED |
| F-020 | Implement lead scoring algorithm | HIGH | NOT_STARTED |
| F-021 | Extract contact info from conversations | HIGH | NOT_STARTED |
| F-022 | Detect purchase intent | HIGH | NOT_STARTED |
| F-023 | Create LeadRepository | MEDIUM | NOT_STARTED |
| F-024 | Implement LeadService | MEDIUM | NOT_STARTED |
| F-025 | Create Leads admin UI | MEDIUM | NOT_STARTED |
| F-026 | Add lead filtering and sorting | LOW | NOT_STARTED |

### Deliverables
- Lead scoring system
- Lead extraction from conversations
- Leads admin UI (API-014 to API-016)

### Dependencies
- Phase 4 completed (conversations exist)

---

## Phase 6: Follow-up Approval

**Duration**: 3-4 days
**Status**: NOT_STARTED

### Objectives
- Generate follow-up messages
- Approval workflow for follow-ups
- Admin review UI

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-027 | Create FollowUp entity | HIGH | NOT_STARTED |
| F-028 | Implement follow-up message generation | HIGH | NOT_STARTED |
| F-029 | Create Approval entity | HIGH | NOT_STARTED |
| F-030 | Implement approval workflow | HIGH | NOT_STARTED |
| F-031 | Create ApprovalController | HIGH | NOT_STARTED |
| F-032 | Build approval queue UI | MEDIUM | NOT_STARTED |
| F-033 | Add approve/reject actions | MEDIUM | NOT_STARTED |
| F-034 | Send follow-up notifications | LOW | NOT_STARTED |

### Deliverables
- Follow-up generation
- Approval workflow
- Approvals admin UI (API-017 to API-020)

### Dependencies
- Phase 5 completed (leads exist)

---

## Phase 7: Vapi Voice Calls

**Duration**: 4-5 days
**Status**: NOT_STARTED

### Objectives
- Integrate Vapi for AI voice calls
- Make approved calls
- Store transcripts and summaries

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-021 | Integrate Vapi API client | HIGH | NOT_STARTED |
| F-035 | Create VoiceCall entity | HIGH | NOT_STARTED |
| F-036 | Implement call triggering from approvals | HIGH | NOT_STARTED |
| F-037 | Track call status | MEDIUM | NOT_STARTED |
| F-038 | Store call transcripts | MEDIUM | NOT_STARTED |
| F-039 | Generate call summaries | MEDIUM | NOT_STARTED |
| F-040 | Create VoiceCallController | MEDIUM | NOT_STARTED |
| F-041 | Build voice calls admin UI | MEDIUM | NOT_STARTED |
| F-042 | Display call history | LOW | NOT_STARTED |

### Deliverables
- Vapi integration
- Voice call management
- Voice calls admin UI (API-021 to API-022)

### Dependencies
- Phase 6 completed (approvals exist)

---

## Phase 8: ElevenLabs Reports

**Duration**: 3-4 days
**Status**: NOT_STARTED

### Objectives
- Generate daily voice reports
- Convert to speech with ElevenLabs
- Report distribution

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-024 | Integrate ElevenLabs API | HIGH | NOT_STARTED |
| F-043 | Create Report entity | HIGH | NOT_STARTED |
| F-044 | Implement report generation logic | HIGH | NOT_STARTED |
| F-045 | Schedule daily reports | MEDIUM | NOT_STARTED |
| F-046 | Convert text to speech | MEDIUM | NOT_STARTED |
| F-047 | Store audio files | MEDIUM | NOT_STARTED |
| F-048 | Create ReportController | MEDIUM | NOT_STARTED |
| F-049 | Add report download UI | LOW | NOT_STARTED |

### Deliverables
- Daily report generation
- ElevenLabs TTS integration
- Report API (API-024 to API-025)

### Dependencies
- Phase 7 completed (calls exist)

---

## Phase 9: Apify Lead Finder

**Duration**: 3-4 days
**Status**: NOT_STARTED

### Objectives
- Find potential leads using Apify
- Lead enrichment
- Import leads into system

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-025 | Integrate Apify API | HIGH | NOT_STARTED |
| F-050 | Configure Apify actors for lead finding | HIGH | NOT_STARTED |
| F-051 | Implement lead discovery service | HIGH | NOT_STARTED |
| F-052 | Enrich discovered leads | MEDIUM | NOT_STARTED |
| F-053 | Import leads to database | MEDIUM | NOT_STARTED |
| F-054 | Create DiscoveryController | MEDIUM | NOT_STARTED |
| F-055 | Add lead discovery UI | LOW | NOT_STARTED |

### Deliverables
- Apify integration
- Lead discovery service
- Discovery API (API-026)

### Dependencies
- Phase 5 completed (lead schema exists)

---

## Phase 10: Evaluation Agent

**Duration**: 2-3 days
**Status**: NOT_STARTED

### Objectives
- Track all agent actions
- Agent performance metrics
- Quality evaluation

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-026 | Create AgentLog entity | HIGH | NOT_STARTED |
| F-056 | Implement agent action interceptor | HIGH | NOT_STARTED |
| F-057 | Log all agent operations | HIGH | NOT_STARTED |
| F-058 | Implement error tracking | MEDIUM | NOT_STARTED |
| F-059 | Calculate performance metrics | MEDIUM | NOT_STARTED |
| F-060 | Create AgentLogController | MEDIUM | NOT_STARTED |
| F-061 | Build agent logs UI | MEDIUM | NOT_STARTED |
| F-062 | Add log filtering and search | LOW | NOT_STARTED |

### Deliverables
- Agent action logging
- Performance metrics
- Agent logs UI (API-023)

### Dependencies
- All previous phases (agents to log exist)

---

## Phase 11: Deployment

**Duration**: 4-5 days
**Status**: NOT_STARTED

### Objectives
- Production deployment
- Monitoring setup
- Security hardening
- Performance optimization

### Tasks

| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-063 | Set up production database | HIGH | NOT_STARTED |
| F-064 | Configure production Redis | HIGH | NOT_STARTED |
| F-065 | Set up CI/CD pipeline | HIGH | NOT_STARTED |
| F-066 | Configure HTTPS/SSL | HIGH | NOT_STARTED |
| F-067 | Set up monitoring (Prometheus + Grafana) | MEDIUM | NOT_STARTED |
| F-068 | Configure log aggregation | MEDIUM | NOT_STARTED |
| F-069 | Security audit | MEDIUM | NOT_STARTED |
| F-070 | Performance testing | MEDIUM | NOT_STARTED |
| F-071 | Load testing | LOW | NOT_STARTED |
| F-072 | Create deployment guide | LOW | NOT_STARTED |

### Deliverables
- Production deployment
- Monitoring dashboard
- Deployment documentation

### Dependencies
- All previous phases completed

---

## Timeline Summary

| Phase | Duration | Start | End | Status |
|-------|----------|-------|-----|--------|
| Phase 0 | 1 day | 2026-07-01 | 2026-07-01 | ✅ DONE |
| Phase 1 | 4 days | TBD | TBD | NOT_STARTED |
| Phase 2 | 4 days | TBD | TBD | NOT_STARTED |
| Phase 3 | 7 days | TBD | TBD | NOT_STARTED |
| Phase 4 | 5 days | TBD | TBD | NOT_STARTED |
| Phase 5 | 4 days | TBD | TBD | NOT_STARTED |
| Phase 6 | 4 days | TBD | TBD | NOT_STARTED |
| Phase 7 | 5 days | TBD | TBD | NOT_STARTED |
| Phase 8 | 4 days | TBD | TBD | NOT_STARTED |
| Phase 9 | 4 days | TBD | TBD | NOT_STARTED |
| Phase 10 | 3 days | TBD | TBD | NOT_STARTED |
| Phase 11 | 5 days | TBD | TBD | NOT_STARTED |

**Total Estimated Duration**: 50 days (~10 weeks)

---

## Release Milestones

- **v0.1.0**: Phase 0 - Foundation ✅
- **v0.2.0**: Phase 1 - Business Management
- **v0.3.0**: Phase 2 - Website Crawling
- **v0.4.0**: Phase 3 - RAG System
- **v0.5.0**: Phase 4 - Chat Agent
- **v0.6.0**: Phase 5 - Lead Qualification
- **v0.7.0**: Phase 6 - Follow-up Approval
- **v0.8.0**: Phase 7 - Voice Calls
- **v0.9.0**: Phase 8 - Reports
- **v1.0.0**: Phase 9-11 - Full Release