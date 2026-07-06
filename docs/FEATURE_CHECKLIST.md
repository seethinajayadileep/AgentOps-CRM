# Feature Checklist

| Feature ID | Feature Name | Status | Backend Done | Frontend Done | API Done | DB Done | Tests Done | Notes |
|------------|--------------|--------|--------------|---------------|---------|---------|------------|-------|
| F-000 | Project Foundation | DONE | ✅ | ✅ | ✅ | ✅ | ❌ | Initial project setup, monorepo structure, docs |
| F-001 | Database Foundation | DONE | ✅ | N/A | N/A | ✅ | ❌ | JPA entities, enums, repositories, Flyway migrations |
| F-002 | Business CRUD | DONE | ✅ | ✅ | ✅ | ✅ | ❌ | Create, Read, Update, Delete businesses |
| F-003 | Website Crawling | DONE | ✅ | ✅ | ✅ | ✅ | ❌ | Firecrawl integration for website crawling |
| F-004 | RAG Knowledge Base | DONE | ✅ | ✅ | ✅ | ✅ | ✅ | Chunking, embedding, semantic search, RAG answers (F-005) |
| F-005 | Support Chat Agent | DONE | ✅ | ✅ | ✅ | ✅ | ❌ | Conversational support agent using RAG knowledge base |
| F-006 | Lead Qualification Agent | DONE | ✅ | ✅ | ✅ | ✅ | ❌ | AI agent to detect buying intent, extract lead info, score, and create/update leads |
| F-007 | Follow-up Approval System | DONE | ✅ | ✅ | ✅ | ✅ | ❌ | Generate AI follow-up messages for qualified leads with human approval workflow |
| F-008 | Voice Call Automation (Vapi) | IN_PROGRESS | ✅ | ✅ | ✅ | ✅ | ❌ | Vapi voice call integration - status constraint fixed (C-018), ready for testing |
| F-009 | Vector Database | IN_PROGRESS | ✅ | N/A | N/A | ✅ | ✅ | F-004: interim `postgres-text` (embedding TEXT + in-memory cosine); pgvector upgrade pending (D-023) |
| F-010 | Embedding Service | DONE | ✅ | N/A | N/A | ✅ | ✅ | F-004: OpenAI text-embedding-3-small (1536-dim), batched |
| F-011 | Knowledge Indexing | DONE | ✅ | ✅ | ✅ | ✅ | ✅ | F-004: chunk + embed + store per business (Build KB) |
| F-012 | Semantic Search | DONE | ✅ | ✅ | ✅ | ✅ | ✅ | F-004: cosine similarity, business-scoped, topK |
| F-013 | RAG Query Engine | DONE | ✅ | ✅ | ✅ | ✅ | ✅ | F-004: retrieval + grounded answer (POST /api/rag/answer), sources, no-hallucination fallback |
| F-014 | Chat Widget UI | NOT_STARTED | N/A | ❌ | N/A | N/A | ❌ | React chat component |
| F-015 | WebSocket Chat | NOT_STARTED | ❌ | ❌ | ❌ | ❌ | ❌ | Real-time chat communication |
| F-016 | Conversation Storage | NOT_STARTED | ❌ | N/A | N/A | ✅ | ❌ | Store chat history |
| F-017 | Lead Scoring Algorithm | NOT_STARTED | ❌ | N/A | N/A | ❌ | ❌ | Qualify leads from conversations |
| F-018 | Lead Extraction | NOT_STARTED | ❌ | N/A | N/A | ❌ | ❌ | Extract contact info and intent |
| F-019 | Lead Management | NOT_STARTED | ❌ | ❌ | ❌ | ✅ | ❌ | CRUD operations for leads |
| F-020 | Vapi Integration | NOT_STARTED | ❌ | N/A | N/A | ❌ | ❌ | Voice call service |
| F-021 | Approval Workflow | NOT_STARTED | ❌ | ❌ | ❌ | ✅ | ❌ | UI and backend for call approvals |
| F-022 | Call Recording | NOT_STARTED | ❌ | N/A | N/A | ✅ | ❌ | Store transcripts and summaries |
| F-023 | ElevenLabs Reports | NOT_STARTED | ❌ | N/A | N/A | ❌ | ❌ | Generate voice reports |
| F-024 | Apify Lead Discovery | DONE | ✅ | ✅ | ✅ | ✅ | ✅ (documented) | Delivered as task **"F-010 Apify Lead Finder"**. Outbound lead discovery via Apify: LeadSourceRun + DiscoveredLead entities, ApifyClient (isolated), LeadFinderService (start/sync/normalize/dedupe/score/import/reject + AgentLog), LeadFinderController, `/lead-finder` UI. Graceful "Apify is not configured." handling. See CHANGELOG C-019, DECISIONS D-024. |
| F-025 | Agent Logging | NOT_STARTED | ❌ | ❌ | N/A | ✅ | ❌ | Track all agent actions |
| F-026 | Evaluation Agent | DONE | ✅ | ✅ | ✅ | N/A | ❌ | "Phase 8 / F-008 Evaluation Agent" task. Safety gate that checks every Support Chat Agent draft answer for grounding/hallucination before sending. EvaluationAgent (LLM + rule-based fallback) + EvaluationService (AgentLog audit) integrated into `/api/chat/ask`; optional `POST /api/evaluation/test`. See CHANGELOG C-020, DECISIONS D-025. |

## F-010 (task label): Apify Lead Finder - Implementation Plan
> Tracked in the table above under **F-024 Apify Lead Discovery** (the row **F-010 = Embedding Service** is a
> separate, already-completed RAG sub-feature). This section documents the "F-010 Apify Lead Finder" task.

### Backend Tasks
- [x] Enums: `LeadSourceRunStatus`, `DiscoveredLeadStatus`
- [x] Entities: `LeadSourceRun`, `DiscoveredLead`
- [x] Repositories: `LeadSourceRunRepository`, `DiscoveredLeadRepository` (+ dedupe methods on `LeadRepository`)
- [x] `ApifyClient` (isolates all Apify-specific input/output; normalized `ApifyLeadResult`)
- [x] Config: `apify.*` in `application.yml`, env files, `run.sh`
- [x] DTOs: `StartLeadFinderRunRequest`, `LeadSourceRunResponse`, `DiscoveredLeadResponse`, `ImportDiscoveredLeadRequest`, `BulkImportDiscoveredLeadsRequest`, `BulkImportResultResponse`, `LeadFinderStartResponse`
- [x] `LeadFinderService` (start, sync, normalize, dedupe, score, import, bulk import, reject, AgentLog)
- [x] `LeadFinderController`
- [x] Migration `V7__add_lead_finder_tables.sql`

### Frontend Tasks
- [x] `types/leadFinder.ts`, `api/leadFinderApi.ts`
- [x] `pages/LeadFinder.tsx` (search form + runs table)
- [x] `pages/LeadFinderResults.tsx` (discovered leads table, import/reject/bulk, raw viewer)
- [x] Routes in `App.tsx`, sidebar nav item

### API Endpoints
- [x] POST /api/lead-finder/runs
- [x] GET /api/lead-finder/runs
- [x] GET /api/lead-finder/runs/{id}
- [x] GET /api/lead-finder/runs/{id}/results
- [x] POST /api/lead-finder/runs/{id}/sync
- [x] POST /api/lead-finder/discovered-leads/{id}/import
- [x] POST /api/lead-finder/discovered-leads/import-bulk
- [x] POST /api/lead-finder/discovered-leads/{id}/reject
- [x] GET /api/lead-finder/config (reports Apify configured status; no secrets)

### AgentLog actions
- APIFY_LEAD_SEARCH_STARTED / COMPLETED / FAILED
- DISCOVERED_LEAD_IMPORTED / DISCOVERED_LEAD_REJECTED / DUPLICATE_LEAD_SKIPPED

## F-008 (task label): Evaluation Agent (Phase 8) - Implementation Plan
> Tracked in the table above under **F-026 Evaluation Agent**. The row **F-008 = Voice Call
> Automation (Vapi)** is a separate feature; this section documents the "Phase 8 / F-008
> Evaluation Agent" task requested for the Support Chat Agent safety gate.

### Backend Tasks
- [x] Create prompt file: `prompts/evaluation-agent.md`
- [x] Create DTOs: `EvaluationRequest`, `EvaluationResponse`
- [x] Create `EvaluationAgent.java` (LLM evaluation + deterministic rule-based fallback + risky keyword groups)
- [x] Create `EvaluationService.java` (orchestration + AgentLog audit trail, fail-closed)
- [x] Create `EvaluationController.java` (optional `POST /api/evaluation/test`)
- [x] Update `ChatService.java` - evaluate draft answer before saving/sending; replace unsafe with safe fallback
- [x] Update `ChatController.AskResponse` - add nested `evaluation` object (keeps `leadDetected`/`leadId`)
- [x] AgentLog actions: EVALUATION_STARTED / EVALUATION_COMPLETED / EVALUATION_FAILED / UNSAFE_ANSWER_BLOCKED / FALLBACK_ANSWER_USED / FALLBACK_USED

### Frontend Tasks
- [x] Update `api/chat.ts` - add `EvaluationSummary` type + `evaluation` field on `AskResponse`
- [x] Update `pages/SupportChat.tsx` - show evaluation badge (LOW=green, MEDIUM=yellow, HIGH=red) + reason; null-safe

### API Endpoints
- [x] POST /api/evaluation/test - evaluate a draft answer against retrieved chunks
- [x] POST /api/chat/ask (updated) - response now includes `evaluation` object

### Testing
- [x] Backend Test 1: safe answer -> safeToSend=true, LOW
- [x] Backend Test 2: invented pricing -> safeToSend=false, HIGH, fallback
- [x] Backend Test 3: unsupported discount -> safeToSend=false
- [x] Backend Test 4: empty chunks -> safeToSend=false, fallback
- [x] Backend Test 5: chat integration -> response includes evaluation, AgentLog created, leadDetected preserved
- [x] Frontend: badge renders; fallback + HIGH badge on pricing; UI does not crash if evaluation is null

## F-007: Follow-up Approval System - Implementation Plan

### Backend Tasks
- [ ] Create prompt file: `prompts/follow-up-agent.md`
- [ ] Create DTOs: FollowUpGenerateRequest, FollowUpGenerateResponse, ApprovalResponse, ApprovalStatusUpdateRequest
- [ ] Create `FollowUpAgent.java` - AI agent for generating follow-up messages
- [ ] Create `FollowUpService.java` - orchestration for follow-up generation
- [ ] Create `ApprovalService.java` - approval workflow management
- [ ] Create `ApprovalController.java` - REST endpoints for approvals

### Frontend Tasks
- [ ] Create `types/approval.ts` - TypeScript interfaces
- [ ] Create `api/approvalsApi.ts` - API client functions
- [ ] Create `ApprovalStatusBadge.tsx` - status badge component
- [ ] Create `ApprovalCard.tsx` - approval card component
- [ ] Create `ApprovalsPage.tsx` - approvals list page
- [ ] Update `LeadDetailPage.tsx` - add follow-up generation UI

### API Endpoints
- [ ] POST /api/leads/{leadId}/follow-up/generate - generate follow-up messages
- [ ] GET /api/approvals - list approvals with filters
- [ ] GET /api/approvals/{id} - get single approval
- [ ] PUT /api/approvals/{id}/approve - approve an approval
- [ ] PUT /api/approvals/{id}/reject - reject an approval
- [ ] PUT /api/approvals/{id}/status - update approval status

### Testing
- [ ] Manual test: generate follow-up for qualified lead
- [ ] Manual test: approve approval
- [ ] Manual test: reject approval
- [ ] Manual test: list approvals
- [ ] Manual test: filter pending approvals
- [ ] Frontend test: generate follow-up UI
- [ ] Frontend test: approvals page, copy, approve, reject

## F-006: Lead Qualification Agent - Implementation Plan

### Backend Tasks
- [ ] Create prompt file: `prompts/lead-qualification-agent.md`
- [ ] Create `LeadQualificationAgent.java` - AI agent for intent detection and extraction
- [ ] Create `LeadQualificationService.java` - orchestration, scoring, duplicate prevention
- [ ] Create `LeadController.java` - REST endpoints for lead management
- [ ] Create DTOs: LeadQualificationRequest, LeadQualificationResponse, LeadResponse, LeadStatusUpdateRequest
- [ ] Update `ChatService.java` - integrate lead qualification after message save
- [ ] Update `ChatController` response DTO - add leadDetected and leadId fields

### Frontend Tasks
- [ ] Create `types/lead.ts` - TypeScript interfaces
- [ ] Create `api/leadsApi.ts` - API client functions
- [ ] Create `LeadStatusBadge.tsx` - status badge component
- [ ] Create `LeadScoreBadge.tsx` - score badge component
- [ ] Create `LeadsPage.tsx` - leads list page
- [ ] Create `LeadDetailPage.tsx` - lead detail page with status update
- [ ] Update `App.tsx` - add leads routing

### API Endpoints
- [ ] POST /api/leads/qualify - qualify lead from message
- [ ] GET /api/leads - list all leads
- [ ] GET /api/leads/{id} - get single lead
- [ ] PUT /api/leads/{id}/status - update lead status

### Testing
- [ ] Manual test: no buying intent
- [ ] Manual test: buying intent without contact
- [ ] Manual test: qualified lead
- [ ] Manual test: hot lead
- [ ] Manual test: duplicate prevention
- [ ] Manual test: status update
- [ ] Frontend test: leads page, detail page, status update

## Legend
- **DONE**: Feature completed and tested
- **IN_PROGRESS**: Currently being developed
- **NOT_STARTED**: Not yet started
- **BLOCKED**: Waiting on dependency
- **NEEDS_TESTING**: Code complete, needs verification

## Statistics
- Total Features: 28
- Completed: 10
- In Progress: 0
- Not Started: 18
- Blocked: 0

## Notes
- **F-004 naming**: Phase 4 is labelled "RAG Knowledge Base" in the roadmap/task. In this
  checklist the RAG work is tracked under rows **F-009–F-013** (Vector DB, Embedding Service,
  Knowledge Indexing, Semantic Search, RAG Query Engine). The row `F-004 = Security Layer` is a
  separate, still-unstarted feature. See docs/CHANGELOG.md C-013 and docs/DECISIONS.md D-023.