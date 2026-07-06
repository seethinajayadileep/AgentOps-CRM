# File Map

| File Path | Layer | Purpose | Related Feature ID | Last Updated |
|-----------|-------|---------|-------------------|--------------|
| **Root** | | | | | |
| README.md | Documentation | Project overview and quick start | F-000 | 2026-07-01 |
| .gitignore | Configuration | Git ignore patterns | F-000 | 2026-07-01 |
| PHASE_0_SUMMARY.md | Documentation | Phase 0 completion summary | F-000 | 2026-07-01 |
| **Backend - Entity Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/entity/BaseEntity.java | Entity | Base entity with UUID and createdAt | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/AuditableEntity.java | Entity | Base entity with createdAt and updatedAt | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/Business.java | Entity | Business entity for customer businesses | F-001, F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/Document.java | Entity | Crawled webpage storage | F-007 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/KnowledgeChunk.java | Entity | RAG chunks (content, sourceUrl, embedding TEXT, chunkIndex) | F-011, F-004 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/entity/Conversation.java | Entity | Customer conversations | F-016 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/Message.java | Entity | Individual messages | F-016 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/Lead.java | Entity | Qualified potential customers | F-019 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/AgentLog.java | Entity | Agent action audit trail | F-025 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/Approval.java | Entity | Approval requests for sensitive actions | F-021 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/VoiceCall.java | Entity | Voice call records from Vapi | F-022 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/LeadSourceRun.java | Entity | Outbound lead discovery run (Apify) | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/entity/DiscoveredLead.java | Entity | Normalized prospect discovered via Apify | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/entity/enums/LeadSourceRunStatus.java | Enum | PENDING/RUNNING/COMPLETED/FAILED | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/entity/enums/DiscoveredLeadStatus.java | Enum | NEW/REVIEWED/IMPORTED/REJECTED | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/repository/LeadSourceRunRepository.java | Repository | Lead source runs | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/repository/DiscoveredLeadRepository.java | Repository | Discovered leads + dedupe queries | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/client/ApifyClient.java | Client | Isolates all Apify API + normalization | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/service/LeadFinderService.java | Service | Start/sync/import/reject + scoring + AgentLog | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/controller/LeadFinderController.java | Controller | Lead Finder REST endpoints | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/StartLeadFinderRunRequest.java | DTO | Start-run request | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/LeadSourceRunResponse.java | DTO | Run response | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/DiscoveredLeadResponse.java | DTO | Discovered lead response | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/ImportDiscoveredLeadRequest.java | DTO | Single import request | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/BulkImportDiscoveredLeadsRequest.java | DTO | Bulk import request | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/BulkImportResultResponse.java | DTO | Bulk import summary | F-010/F-024 | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/LeadFinderStartResponse.java | DTO | Start summary (runId/status/message) | F-010/F-024 | 2026-07-03 |
| backend/src/main/resources/migration/V7__add_lead_finder_tables.sql | Migration | lead_source_runs + discovered_leads tables | F-010/F-024 | 2026-07-03 |
| frontend/src/types/leadFinder.ts | Frontend Types | Lead Finder TS types | F-010/F-024 | 2026-07-03 |
| frontend/src/api/leadFinderApi.ts | Frontend API | Lead Finder API client | F-010/F-024 | 2026-07-03 |
| frontend/src/pages/LeadFinder.tsx | Frontend Page | Search form + runs list | F-010/F-024 | 2026-07-03 |
| frontend/src/pages/LeadFinderResults.tsx | Frontend Page | Discovered leads + import/reject/bulk | F-010/F-024 | 2026-07-03 |
| **Backend - Enum Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/entity/enums/CrawlStatus.java | Enum | Website crawl status values | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/Channel.java | Enum | Communication channel types | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/ConversationStatus.java | Enum | Conversation status values | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/MessageRole.java | Enum | Message role types | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/LeadStatus.java | Enum | Lead status values | F-019 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/Urgency.java | Enum | Lead requirement urgency levels | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/Timeline.java | Enum | Lead timeline values | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/AgentActionStatus.java | Enum | Agent action status values | F-001 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/ApprovalType.java | Enum | Approval request types | F-001, F-021 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/ApprovalStatus.java | Enum | Approval status values | F-001, F-021 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java | Enum | Voice call status values | F-022 | 2026-07-01 |
| **Backend - Controller Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/controller/HealthController.java | Controller | Health check endpoint | F-005 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/controller/BusinessController.java | Controller | Business CRUD endpoints | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/controller/CrawlController.java | Controller | Website crawl endpoints | F-003 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/controller/RagController.java | Controller | KB build + RAG search endpoints | F-004 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/controller/ChatController.java | Controller | Support chat endpoints (POST /api/chat/ask, GET history) | F-005 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/controller/LeadController.java | Controller | Lead CRUD and qualification endpoints | F-006 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/controller/ApprovalController.java | Controller | Approval management and follow-up generation endpoints | F-007 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/controller/EvaluationController.java | Controller | Evaluation Agent test endpoint (POST /api/evaluation/test) | F-008 (F-026) | 2026-07-03 |
| **Backend - Agents** | | | | | |
| backend/src/main/java/com/agentopscrm/agent/LeadQualificationAgent.java | Agent | AI intent detection + lead extraction | F-006 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/agent/FollowUpAgent.java | Agent | AI follow-up message generation | F-007 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/agent/EvaluationAgent.java | Agent | Evaluate draft answers (LLM + rule-based fallback, risky-keyword groups) | F-008 (F-026) | 2026-07-03 |
| **Backend - Service Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/service/BusinessService.java | Service | Business business logic | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/service/CrawlService.java | Service | Website crawl business logic | F-003 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/service/ChatService.java | Service | Support chat: conversation/message management, RAG integration, answer generation | F-005 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/ChunkingService.java | Service | Split document content into overlapping chunks | F-004 (F-011) | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/EmbeddingService.java | Service | Generate OpenAI embeddings (text-embedding-3-small) | F-004 (F-010) | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/VectorStoreService.java | Service | Vector (de)serialize + cosine similarity | F-004 (F-009) | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/KnowledgeBaseService.java | Service | Build KB: chunk + embed + store per business | F-004 (F-011) | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/RagService.java | Service | Business-scoped semantic RAG search + grounded answer() | F-004 (F-012/F-013) | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/AnswerService.java | Service | LLM answer generation (OpenAI chat), grounded/no-hallucination | F-004 (F-013) | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/LeadQualificationService.java | Service | Lead qualification: intent detection, extraction, scoring | F-006 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/FollowUpService.java | Service | Follow-up message generation orchestration | F-007 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/ApprovalService.java | Service | Approval workflow management (CRUD, status updates, logging) | F-007 | 2026-07-02 |
| backend/src/main/java/com/agentopscrm/service/EvaluationService.java | Service | Evaluation Agent orchestration + AgentLog audit (fail-closed) | F-008 (F-026) | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/service/AgentLogService.java | Service | Agent logs observability: filtering, pagination, summary statistics | F-012 | 2026-07-05 |
| backend/src/main/java/com/agentopscrm/controller/AgentLogController.java | Controller | Agent logs REST endpoints (GET /api/agent-logs, summary) | F-012 | 2026-07-05 |
| backend/src/main/java/com/agentopscrm/dto/AgentLogResponse.java | DTO | Detailed agent execution response | F-012 | 2026-07-05 |
| backend/src/main/java/com/agentopscrm/dto/AgentLogSummaryResponse.java | DTO | Summary statistics response | F-012 | 2026-07-05 |
| frontend/src/types/agentLog.ts | Frontend Types | Agent logs TypeScript types | F-012 | 2026-07-05 |
| frontend/src/api/agentLogsApi.ts | Frontend API | Agent logs API client | F-012 | 2026-07-05 |
| frontend/src/pages/AgentLogs.tsx | Frontend Page | Agent logs observability page | F-012 | 2026-07-05 |
| frontend/src/components/agent-logs/ExecutionDetailsModal.tsx | Frontend Component | Execution details modal with tabs (overview/input/output/error) | F-012 | 2026-07-05 |
| **Backend - DTOs (Evaluation)** | | | | | |
| backend/src/main/java/com/agentopscrm/dto/EvaluationRequest.java | DTO | Evaluation Agent request (question, draftAnswer, retrievedChunks, sourceUrls) | F-008 (F-026) | 2026-07-03 |
| backend/src/main/java/com/agentopscrm/dto/EvaluationResponse.java | DTO | Evaluation verdict (confidenceScore, hallucinationRisk, safeToSend, reason, finalAnswer) | F-008 (F-026) | 2026-07-03 |
| **Backend - Prompts** | | | | | |
| backend/src/main/resources/prompts/support-agent.md | Prompt | System prompt for support chat agent with rules and guidelines | F-005 | 2026-07-02 |
| backend/src/main/resources/prompts/lead-qualification-agent.md | Prompt | System prompt for lead qualification agent | F-006 | 2026-07-02 |
| backend/src/main/resources/prompts/follow-up-agent.md | Prompt | System prompt for follow-up message generation agent | F-007 | 2026-07-02 |
| backend/src/main/resources/prompts/evaluation-agent.md | Prompt | System prompt for the Evaluation Agent (grounding/safety, strict JSON) | F-008 (F-026) | 2026-07-03 |
| **Backend - Migrations** | | | | | |
| backend/src/main/resources/migration/V2__add_knowledge_chunk_embedding.sql | Migration | Adds embedding TEXT + chunk_index to knowledge_chunks | F-004 | 2026-07-02 |
| backend/src/main/resources/migration/V3__add_lead_capture_fields.sql | Migration | Adds lead capture fields from F-006 | F-006 | 2026-07-02 |
| backend/src/main/resources/migration/V4__add_approval_style.sql | Migration | Adds style field to approvals table | F-007 | 2026-07-02 |
| backend/src/main/resources/migration/V8__fix_agent_logs_status_constraint.sql | Migration | Recreates agent_logs status CHECK to include FALLBACK_USED (B-005) | F-008 (F-026) | 2026-07-03 |
| **Backend - Tests** | | | | | |
| backend/src/test/java/com/agentopscrm/service/RagServiceTest.java | Test | RAG search: isolation, validation, failure handling | F-004 | 2026-07-02 |
| backend/src/test/java/com/agentopscrm/service/KnowledgeBaseServiceTest.java | Test | KB build: success, empty docs, invalid business, embedding failure | F-004 | 2026-07-02 |
| **Frontend - RAG & Chat** | | | | | |
| frontend/src/api/rag.ts | API Client | KB build + RAG search calls and types | F-004 | 2026-07-02 |
| frontend/src/api/chat.ts | API Client | Support chat API calls (askQuestion, getConversationHistory) | F-005 | 2026-07-02 |
| frontend/src/api/leadsApi.ts | API Client | Lead management API calls | F-006 | 2026-07-02 |
| frontend/src/api/approvalsApi.ts | API Client | Approval management and follow-up generation API calls | F-007 | 2026-07-02 |
| frontend/src/pages/BusinessDetail.tsx | Page | Business detail: crawl, Build KB, RAG test search, Test Chat button | F-002, F-003, F-004, F-005 | 2026-07-02 |
| frontend/src/pages/SupportChat.tsx | Page | Test chat page with message display, confidence scores, sources | F-005 | 2026-07-02 |
| frontend/src/pages/LeadsPage.tsx | Page | Leads list page with filters and status badges | F-006 | 2026-07-02 |
| frontend/src/pages/LeadDetailPage.tsx | Page | Lead detail page with status update and follow-up generation | F-006, F-007 | 2026-07-02 |
| frontend/src/pages/ApprovalsPage.tsx | Page | Approvals page with filters, approve/reject actions | F-007 | 2026-07-02 |
| frontend/src/components/leads/LeadStatusBadge.tsx | Component | Status badge for leads | F-006 | 2026-07-02 |
| frontend/src/components/leads/LeadScoreBadge.tsx | Component | Score badge for leads | F-006 | 2026-07-02 |
| frontend/src/components/approvals/ApprovalStatusBadge.tsx | Component | Status badge for approvals | F-007 | 2026-07-02 |
| frontend/src/components/approvals/ApprovalCard.tsx | Component | Approval card with approve/reject/copy buttons | F-007 | 2026-07-02 |
| frontend/src/types/lead.ts | Types | Lead-related TypeScript interfaces | F-006 | 2026-07-02 |
| frontend/src/types/approval.ts | Types | Approval-related TypeScript interfaces | F-007 | 2026-07-02 |
| **Backend - Repository Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/repository/BusinessRepository.java | Repository | Business data access | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/DocumentRepository.java | Repository | Document data access | F-007 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/KnowledgeChunkRepository.java | Repository | Knowledge chunk data access | F-011 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/ConversationRepository.java | Repository | Conversation data access | F-016 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/MessageRepository.java | Repository | Message data access | F-016 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/LeadRepository.java | Repository | Lead data access | F-019 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/AgentLogRepository.java | Repository | Agent log data access | F-025 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/ApprovalRepository.java | Repository | Approval data access | F-021 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/repository/VoiceCallRepository.java | Repository | Voice call data access | F-022 | 2026-07-01 |
| **Backend - Exception Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/exception/BusinessNotFoundException.java | Exception | Business not found exception | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/exception/BusinessAlreadyExistsException.java | Exception | Duplicate business URL exception | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/exception/GlobalExceptionHandler.java | Exception | Global error handler | F-002 | 2026-07-01 |
| **Backend - DTO Layer** | | | | | |
| backend/src/main/java/com/agentopscrm/dto/HealthResponse.java | DTO | Health response DTO | F-005 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/ServiceStatus.java | DTO | Service status DTO | F-005 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/CreateBusinessRequest.java | DTO | Create business request | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/UpdateBusinessRequest.java | DTO | Update business request | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/BusinessResponse.java | DTO | Business response DTO | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/ApiResponse.java | DTO | Generic API response | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/PaginatedResponse.java | DTO | Paginated response DTO | F-002 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/dto/PaginationMeta.java | DTO | Pagination metadata DTO | F-002 | 2026-07-01 |
| **Backend - Configuration** | | | | | |
| backend/pom.xml | Configuration | Maven dependencies and build config | F-000 | 2026-07-01 |
| backend/.env.example | Configuration | Environment variables template | F-000 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/AgentOpsCrmApplication.java | Application | Spring Boot main entry point | F-000 | 2026-07-01 |
| backend/src/main/java/com/agentopscrm/config/CorsConfig.java | Configuration | CORS configuration for frontend access | F-000 | 2026-07-01 |
| backend/src/main/resources/application.yml | Configuration | Main Spring configuration (YAML) | F-001 | 2026-07-01 |
| backend/src/main/resources/application-dev.yml | Configuration | Development environment config | F-001 | 2026-07-01 |
| backend/src/main/resources/application-prod.yml | Configuration | Production environment config | F-001 | 2026-07-01 |
| backend/src/main/resources/migration/V1__create_tables.sql | Migration | Flyway migration for all tables | F-001 | 2026-07-01 |
| backend/src/main/resources/prompts/ | Resources | AI prompt templates | F-013 | 2026-07-01 |
| **Frontend - API Layer** | | | | | |
| frontend/src/api/health.ts | API | Health API client functions | F-005 | 2026-07-01 |
| frontend/src/api/business.ts | API | Business CRUD API functions | F-002 | 2026-07-01 |
| frontend/src/api/axios.ts | Utility | Axios instance configuration | F-000 | 2026-07-01 |
| **Frontend - Components** | | | | | |
| frontend/src/components/Layout.tsx | Component | Main layout shell | F-000 | 2026-07-01 |
| frontend/src/components/Sidebar.tsx | Component | Sidebar navigation | F-000 | 2026-07-01 |
| frontend/src/components/Header.tsx | Component | App header with search | F-000 | 2026-07-01 |
| **Frontend - Pages** | | | | | |
| frontend/src/main.tsx | View | Root React component | F-000 | 2026-07-01 |
| frontend/src/App.tsx | View | Root React component with routing | F-000, F-002 | 2026-07-01 |
| frontend/src/index.css | Styles | Tailwind imports and base styles | F-000 | 2026-07-01 |
| frontend/src/vite-env.d.ts | Types | Vite type definitions | F-000 | 2026-07-01 |
| frontend/src/pages/Dashboard.tsx | View | Dashboard overview page | F-000 | 2026-07-01 |
| frontend/src/pages/Businesses.tsx | View | Business list page | F-002 | 2026-07-01 |
| frontend/src/pages/AddBusiness.tsx | View | Add business form page | F-002 | 2026-07-01 |
| frontend/src/pages/BusinessDetail.tsx | View | Business detail page | F-002 | 2026-07-01 |
| frontend/src/pages/Leads.tsx | View | Lead management page | F-000 | 2026-07-01 |
| frontend/src/pages/Conversations.tsx | View | Chat conversations page | F-000 | 2026-07-01 |
| frontend/src/pages/VoiceCalls.tsx | View | Voice calls history page | F-000 | 2026-07-01 |
| frontend/src/pages/Approvals.tsx | View | Approval queue page | F-000 | 2026-07-01 |
| frontend/src/pages/AgentLogs.tsx | View | Agent logs audit page | F-000 | 2026-07-01 |
| frontend/src/pages/Settings.tsx | View | Settings configuration page | F-000 | 2026-07-01 |
| **Frontend - Types** | | | | | |
| frontend/src/types/index.ts | Types | TypeScript type definitions | F-000, F-002 | 2026-07-01 |
| **Frontend - Utils** | | | | | |
| frontend/src/utils/axios.ts | Utility | Axios instance configuration | F-000 | 2026-07-01 |
| **Frontend - API** | | | | | |
| docker/docker-compose.yml | Infrastructure | PostgreSQL and Redis services | F-000 | 2026-07-01 |
| **Documentation** | | | | | |
| docs/PROJECT_SPEC.md | Documentation | Complete project specification | F-000 | 2026-07-01 |
| docs/FEATURE_CHECKLIST.md | Documentation | Feature tracking with status | F-002 | 2026-07-01 |
| docs/API_CONTRACT.md | Documentation | API endpoint documentation | F-002 | 2026-07-01 |
| docs/CHANGELOG.md | Documentation | Change history | F-002 | 2026-07-01 |
| docs/DECISIONS.md | Documentation | Technical decisions log | F-000 | 2026-07-01 |
| docs/FILE_MAP.md | Documentation | File purpose mapping | F-002 | 2026-07-01 |
| docs/DEBUG_LOG.md | Documentation | Debug issues log | F-000 | 2026-07-01 |
| docs/TEST_PLAN.md | Documentation | Testing strategies | F-002 | 2026-07-01 |
| docs/ENVIRONMENT.md | Documentation | Environment variables guide | F-000 | 2026-07-01 |
| docs/ROADMAP.md | Documentation | Development roadmap | F-000 | 2026-07-01 |

## Layer Definitions
- **Application**: Main application entry points
- **Entity**: JPA entity classes
- **Enum**: Java enum types
- **Repository**: Spring Data JPA repositories
- **Controller**: REST API controllers
- **DTO**: Data transfer objects
- **View**: Frontend pages and components
- **API**: API client functions
- **Types**: TypeScript type definitions
- **Utility**: Helper functions and utilities
- **Resources**: Resource files (prompts, migrations)
- **Infrastructure**: Docker and infrastructure config
- **Configuration**: Build and runtime configuration files
- **Documentation**: Project documentation