# Test Plan

## Backend Tests

### Unit Tests
- [ ] Entity tests
  - [ ] BaseEntity createdAt initialization
  - [ ] AuditableEntity createdAt and updatedAt initialization
  - [ ] Business entity with all fields
  - [ ] Document entity with relationships
  - [ ] KnowledgeChunk entity with relationships
  - [ ] Conversation entity with relationships
  - [ ] Message entity with role
  - [ ] Lead entity with scoring
  - [ ] AgentLog entity with action logging
  - [ ] Approval entity with workflow
  - [ ] VoiceCall entity with call details

- [ ] Enum tests
  - [ ] CrawlStatus enum values
  - [ ] Channel enum values
  - [ ] ConversationStatus enum values
  - [ ] MessageRole enum values
  - [ ] LeadStatus enum values
  - [ ] Urgency enum values
  - [ ] Timeline enum values
  - [ ] AgentActionStatus enum values
  - [ ] ApprovalType enum values
  - [ ] ApprovalStatus enum values
  - [ ] VoiceCallStatus enum values

- [ ] Service layer tests (to be added)
  - [ ] BusinessService tests
  - [ ] LeadService tests
  - [ ] ConversationService tests
  - [ ] CrawlService tests
  - [x] RagService tests (F-004) — see RagServiceTest
  - [x] KnowledgeBaseService tests (F-004) — see KnowledgeBaseServiceTest

### F-004 RAG Knowledge Base — Unit Tests (11 tests, all passing)
Run: `mvn test` (host on JDK > 21 auto-uses ByteBuddy experimental via pom argLine).

`KnowledgeBaseServiceTest`:
- [x] Build knowledge base for a business (chunks + embeddings persisted, rich metrics)
- [x] Empty crawled documents return a clean, non-error response (`status=NO_DOCUMENTS`)
- [x] Invalid business ID returns a clean error (`BusinessNotFoundException`)
- [x] Embedding provider failure handled (`status=EMBEDDING_FAILED`, nothing saved)
- [x] Embedding provider not configured handled (`status=EMBEDDING_NOT_CONFIGURED`)

`RagServiceTest`:
- [x] Search returns chunks only for the requested business (repo-scoped `findByBusinessId`)
- [x] Search does not leak another business's chunks (other business never queried)
- [x] Empty search query validation (`RagSearchException`, no repo access)
- [x] Invalid business ID returns a clean error (`BusinessNotFoundException`)
- [x] Empty knowledge base returns an empty, clean result
- [x] Embedding provider failure handled (`RagSearchException`)
- [x] Vector store failure handled (`RagSearchException`)
- [x] Answer: strong context → grounded answer + sources (LLM called)
- [x] Answer: weak similarity → fixed fallback WITHOUT calling the LLM (no hallucination)
- [x] Answer: empty KB → `NO_RESULTS` fallback (business-scoped)

`ChunkingServiceTest` (F-004):
- [x] null/empty → empty; short prose → single chunk
- [x] large content terminates with bounded chunks (infinite-loop regression guard)
- [x] navigation/link-list boilerplate flagged; real prose kept

**Total F-004 unit tests: 20/20 passing** (`mvn test`).

- [ ] Repository tests (to be added)
  - [ ] BusinessRepository CRUD operations
  - [ ] BusinessRepository findByWebsiteUrl
  - [ ] BusinessRepository findByCrawlStatus
  - [ ] BusinessRepository search
  - [ ] DocumentRepository findByBusinessId
  - [ ] DocumentRepository existsByUrl
  - [ ] KnowledgeChunkRepository findByBusinessId
  - [ ] ConversationRepository findByBusinessIdAndStatus
  - [ ] MessageRepository findByConversationId
  - [ ] LeadRepository findByEmail
  - [ ] LeadRepository findByMinLeadScore
  - [ ] AgentLogRepository findByStatus
  - [ ] ApprovalRepository findByStatus
  - [ ] VoiceCallRepository findByVapiCallId

- [ ] DTO tests (to be added)
  - [ ] DTO validation tests
  - [ ] DTO serialization/deserialization tests

### Integration Tests
- [ ] Database integration tests
  - [ ] PostgreSQL connection test
  - [ ] Flyway migration test
  - [ ] Entity persistence test
  - [ ] Cascade delete test
  - [ ] Repository integration tests
  - [ ] Index creation verification

- [ ] Redis integration tests
  - [ ] Redis connection test
  - [ ] Cache service tests

- [ ] API integration tests
  - [ ] Health endpoint integration test
  - [ ] Business CRUD integration tests
  - [ ] Chat endpoint integration tests

### Database Tests
- [ ] Schema validation tests
  - [ ] All tables created
  - [ ] All indexes created
  - [ ] Foreign key constraints work
  - [ ] Cascade delete works correctly
  - [ ] Enum values match database

### API Tests
- [ ] Health endpoint tests
  - [ ] GET /api/health returns 200
  - [ ] GET /api/health returns correct JSON structure
  - [ ] GET /api/health includes all required fields
  - [ ] GET /api/health works with PostgreSQL down
  - [ ] GET /api/health works with Redis down

- [ ] Business API tests (to be added)
  - [ ] POST /api/businesses creates business
  - [ ] GET /api/businesses returns list
  - [ ] GET /api/businesses/{id} returns single business
  - [ ] PUT /api/businesses/{id} updates business
  - [ ] DELETE /api/businesses/{id} deletes business

- [ ] Crawl API tests (to be added)
  - [ ] POST /api/businesses/{id}/crawl starts crawl
  - [ ] GET /api/businesses/{id}/documents returns documents
  - [ ] Crawl sets business status correctly
  - [ ] Documents are deduplicated by URL
  - [ ] POST /api/businesses creates business
  - [ ] GET /api/businesses returns list
  - [ ] GET /api/businesses/{id} returns single business
  - [ ] PUT /api/businesses/{id} updates business
  - [ ] DELETE /api/businesses/{id} deletes business

- [ ] Chat API tests (to be added)
  - [ ] POST /api/chat processes message
  - [ ] POST /api/chat returns AI response
  - [ ] POST /api/chat creates conversation if needed

- [ ] Follow-up & Approval API tests (F-007)
  - [ ] POST /api/leads/{leadId}/follow-up/generate creates 3 approvals
  - [ ] POST /api/leads/{leadId}/follow-up/generate with specific tone creates 1 approval
  - [ ] GET /api/approvals returns all approvals
  - [ ] GET /api/approvals?status=PENDING filters by status
  - [ ] GET /api/approvals?type=FOLLOW_UP_MESSAGE filters by type
  - [ ] GET /api/approvals/{id} returns single approval
  - [ ] PUT /api/approvals/{id}/approve updates status to APPROVED
  - [ ] PUT /api/approvals/{id}/reject updates status to REJECTED
  - [ ] PUT /api/approvals/{id}/status updates status flexibly
  - [ ] All approval actions create AgentLog records

## Frontend Tests

### Component Tests
- [ ] Layout component tests
  - [ ] Renders sidebar
  - [ ] Renders header
  - [ ] Navigation works correctly

- [ ] Page component tests
  - [ ] Dashboard renders metrics
  - [ ] Businesses page renders correctly
  - [ ] Leads page renders correctly
  - [ ] Conversations page renders correctly
  - [ ] Voice calls page renders correctly
  - [ ] Approvals page renders correctly
  - [ ] Agent logs page renders correctly
  - [ ] Settings page renders correctly

### Integration Tests
- [ ] Routing tests
  - [ ] Navigation between pages works
  - [ ] URL params handled correctly

- [ ] API client tests
  - [ ] healthApi.getHealth() calls correct endpoint
  - [ ] healthApi.getHealth() parses response correctly

## Manual Tests

### Backend Manual Tests
- [ ] Health endpoint accessible via browser
- [ ] Health endpoint returns valid JSON
- [ ] Actuator health endpoint accessible
- [ ] Server starts without errors
- [ ] Server shuts down gracefully
- [ ] Flyway migrations run successfully on fresh database
- [ ] Flyway validations pass on existing database

### Database Manual Tests
- [ ] All tables created after migration
- [ ] Can insert sample Business record
- [ ] Can query Business with repository
- [ ] Cascade delete works (Business → Documents)
- [ ] Foreign key constraints enforce correctly
- [ ] UUIDs are auto-generated

### Frontend Manual Tests
- [ ] Frontend loads without errors
- [ ] All pages accessible via navigation
- [ ] Sidebar navigation works
- [ ] Header search displays
- [ ] Responsive design on mobile

### Integration Manual Tests
- [ ] Frontend can call backend health endpoint
- [ ] Health data displays correctly on frontend
- [ ] Error handling works (e.g., backend down)

## Integration Tests

### Database Integration
- [ ] PostgreSQL connection pool works
- [ ] Flyway migrations run successfully
- [ ] Entity CRUD operations work
- [ ] Database constraints enforced
- [ ] Cascade delete works for relationships
- [ ] Indexes improve query performance

### Redis Integration
- [ ] Redis connection works
- [ ] Cache operations work
- [ ] Cache expiration works

### External API Integration (to be added)
- [ ] Firecrawl API integration
- [ ] Vapi API integration
- [ ] ElevenLabs API integration
- [ ] Apify API integration

### End-to-End Tests (to be added)
- [ ] Business onboarding flow
- [ ] Website crawling flow
- [ ] Chat conversation flow
- [ ] Lead qualification flow
- [ ] Voice call approval flow

## Test Execution

### Run All Tests
```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test
```

### Run Specific Tests
```bash
# Backend unit tests only
mvn test -Dtest="*EntityTest"
mvn test -Dtest="*RepositoryTest"
mvn test -Dtest="*ServiceTest"

# Backend integration tests only
mvn test -Dtest="*IntegrationTest"

# Frontend specific test
npm test -- components/Layout.test.tsx

# E2E specific suite
npm run test:e2e -- --spec onboarding.spec.ts
```

### Test Database Setup
```bash
# Use test database
export DB_NAME=agentops_crm_test
export DB_USER=postgres
export DB_PASSWORD=postgres
export DB_HOST=localhost

# Run tests
mvn test
```

## Coverage Targets

- **Unit Tests**: Target 80% code coverage
- **Integration Tests**: All critical paths covered
- **E2E Tests**: All user flows covered
- **Repository Tests**: 100% coverage (all methods)

## Test Environment

- **Local Testing**: Docker PostgreSQL, Docker Redis
- **CI Testing**: GitHub Actions or similar
- **Staging Testing**: Staging environment with production-like data

## Database Schema Tests

### Table Creation Tests
- [ ] businesses table created
- [ ] documents table created
- [ ] knowledge_chunks table created
- [ ] conversations table created
- [ ] messages table created
- [ ] leads table created
- [ ] agent_logs table created
- [ ] approvals table created
- [ ] voice_calls table created

### Index Tests
- [ ] All indexes created correctly
- [ ] Index names match specification
- [ ] Index columns are correct

### Constraint Tests
- [ ] Foreign key constraints work
- [ ] Cascade delete works
- [ ] NOT NULL constraints enforced
- [ ] UNIQUE constraints enforced

## F-010 / F-024: Apify Lead Finder Tests

### Backend / Service
- [ ] **T-LF-01** Start run without Apify config → returns clean "Apify is not configured." (HTTP 503), app still runs
- [ ] **T-LF-02** Valid run (Apify configured) creates a `LeadSourceRun` with status RUNNING and stores apifyRunId/datasetId
- [ ] **T-LF-03** Sync saves normalized `DiscoveredLead` rows and sets run status COMPLETED + totalResults
- [ ] **T-LF-04** Duplicate discovered leads within a run are skipped (email → phone → website → name+location)
- [ ] **T-LF-05** Import creates a CRM `Lead` (name = contactName || businessName, requirement = "Outbound prospect discovered via Apify", status NEW, score copied), marks DiscoveredLead IMPORTED, bumps run importedCount
- [ ] **T-LF-06** Duplicate CRM import is prevented → 409 + DUPLICATE_LEAD_SKIPPED AgentLog
- [ ] **T-LF-07** Reject updates discovered lead status to REJECTED + DISCOVERED_LEAD_REJECTED AgentLog
- [ ] **T-LF-08** Bulk import returns counts (imported/skippedDuplicates/failed) and does not abort on a single duplicate/error
- [ ] **T-LF-09** AgentLog entries created for START/COMPLETE/FAIL/IMPORT/REJECT/DUPLICATE actions
- [ ] **T-LF-10** Import without targetBusinessId auto-selects when exactly one business exists; otherwise returns a 400 asking to pick one

### API
- [ ] POST /api/lead-finder/runs (validation: searchName required)
- [ ] GET /api/lead-finder/runs and /runs/{id}
- [ ] GET /api/lead-finder/runs/{id}/results
- [ ] POST /api/lead-finder/runs/{id}/sync
- [ ] POST /api/lead-finder/discovered-leads/{id}/import and /import-bulk
- [ ] POST /api/lead-finder/discovered-leads/{id}/reject
- [ ] GET /api/lead-finder/config returns apifyConfigured (never any token)

### Frontend
- [ ] Lead Finder page shows the search form and runs table
- [ ] "Apify not configured" banner shown and Start button disabled when config false
- [ ] Results page lists discovered leads; import/reject/bulk import update the table
- [ ] Loading, empty, error, and success states render correctly
- [ ] Apify token never appears in any network request from the browser

### How to run backend tests
```bash
cd backend
mvn -q test
```

## F-008 / F-026: Evaluation Agent Tests

Use `POST /api/evaluation/test` for isolated backend checks, and `/api/chat/ask`
(or the Support Chat page) for the integration check. `businessId` must be a real
business UUID that exists in your DB.

### Backend / Service (via POST /api/evaluation/test)
- [ ] **T-EV-01** Safe answer
  - question: "What services do you provide?"
  - retrievedChunks: `["We provide website development, AI chatbot setup, and automation support."]`
  - draftAnswer: "We provide website development, AI chatbot setup, and automation support."
  - Expected: `safeToSend = true`, `hallucinationRisk = LOW`, `finalAnswer = null`
- [ ] **T-EV-02** Invented pricing
  - question: "What is the price?"
  - retrievedChunks: `["We provide website development and AI chatbot setup."]`
  - draftAnswer: "Our website development starts from ₹10000."
  - Expected: `safeToSend = false`, `hallucinationRisk = HIGH`, `finalAnswer` = safe fallback
- [ ] **T-EV-03** Unsupported discount
  - retrievedChunks: `["We provide website development."]`
  - draftAnswer: "We can give you a 50% discount."
  - Expected: `safeToSend = false`
- [ ] **T-EV-04** Empty chunks
  - retrievedChunks: `[]`
  - Expected: `safeToSend = false`, `finalAnswer` = safe fallback
- [ ] **T-EV-05** Support Chat integration (`POST /api/chat/ask`)
  - Ask any business question.
  - Expected: response includes an `evaluation` object; a `SUPPORT_CHAT` AgentLog and
    `EvaluationAgent` AgentLog rows are created; existing `leadDetected`/`leadId` still work.
- [ ] **T-EV-06** LLM-failure fallback
  - Temporarily unset `OPENAI_API_KEY` (or simulate failure) → rule-based verdict returned,
    AgentLog `FALLBACK_USED` recorded, chat still responds.

### AgentLog (agentName = EvaluationAgent)
- [ ] EVALUATION_STARTED and EVALUATION_COMPLETED recorded for every evaluation
- [ ] UNSAFE_ANSWER_BLOCKED + FALLBACK_ANSWER_USED recorded when a draft is unsafe
- [ ] EVALUATION_FAILED recorded (fail-closed) if both LLM and rules throw

### Frontend (Support Chat page)
- [ ] Open Support Chat, ask a normal business question → answer + evaluation badge appear
- [ ] Ask a pricing question when pricing is NOT in the knowledge base → fallback answer shown
- [ ] HIGH-risk badge appears in red for the blocked answer
- [ ] Badge colors: LOW = green, MEDIUM = yellow, HIGH = red
- [ ] UI does not crash when `evaluation` is null (e.g. greeting "hi")

### Example curl (T-EV-02)
```bash
curl -s -X POST http://localhost:8080/api/evaluation/test \
  -H 'Content-Type: application/json' \
  -d '{
    "businessId": "<REAL_BUSINESS_UUID>",
    "question": "What is your price?",
    "draftAnswer": "Our website starts from ₹10000.",
    "retrievedChunks": ["We provide website development and chatbot setup."],
    "sourceUrls": ["https://example.com/services"]
  }'
```