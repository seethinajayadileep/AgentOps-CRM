# API Contract

## Base URL
```
Development: http://localhost:8080
```

## Response Formats

### HealthResponse (API-001)
```json
{
  "status": "UP",
  "timestamp": "2026-07-01T12:00:00Z",
  "services": {
    "database": {
      "status": "UP",
      "message": "Database connection established"
    },
    "redis": {
      "status": "UP",
      "message": "Redis connection established"
    }
  },
  "version": "0.2.0"
}
```

### ErrorResponse
```json
{
  "status": 404,
  "error": "ERROR_CODE",
  "message": "Error description",
  "timestamp": "2026-07-01T00:00:00Z"
}
```

### CrawlResponse
```json
{
  "status": "COMPLETED",
  "message": "Crawl completed. 5 pages saved, 0 duplicates skipped."
}
```

### DocumentResponse
```json
{
  "id": "uuid",
  "url": "https://example.com/page",
  "title": "Page Title",
  "status": "COMPLETED",
  "createdAt": "2026-07-01T12:00:00Z",
  "updatedAt": "2026-07-01T12:00:00Z"
}
```

### PaginatedResponse
```json
{
  "items": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

## APIs

### Health & Monitoring
| API ID | Method | Endpoint | Request Body | Response | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-001 | GET | /api/health | None | `HealthResponse` | ✅ IMPLEMENTED | F-005 |
| API-002 | GET | /actuator/health | None | Actuator health with components | ✅ IMPLEMENTED | F-005 |

### Business Management
| API ID | Method | Endpoint | Request Body | Response | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-003 | POST | /api/businesses | `CreateBusinessRequest` | `BusinessResponse` | ✅ IMPLEMENTED | F-002 |
| API-004 | GET | /api/businesses | Query params (page, size, search) | `PaginatedBusinessResponse` | ✅ IMPLEMENTED | F-002 |
| API-005 | GET | /api/businesses/{id} | None | `BusinessResponse` | ✅ IMPLEMENTED | F-002 |
| API-006 | PUT | /api/businesses/{id} | `UpdateBusinessRequest` | `BusinessResponse` | ✅ IMPLEMENTED | F-002 |
| API-007 | DELETE | /api/businesses/{id} | None | `DeleteResponse` | ✅ IMPLEMENTED | F-002 |
| API-008 | GET | /api/businesses/search | Query params (term, page, size) | `PaginatedBusinessResponse` | ✅ IMPLEMENTED | F-002 |
| API-009 | GET | /api/businesses/crawl-status/{status} | Query params (page, size) | `PaginatedBusinessResponse` | ✅ IMPLEMENTED | F-002 |

### Website Crawling
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-010 | POST | /api/businesses/{id}/crawl | None | `ApiResponse<CrawlResponse>` | ✅ IMPLEMENTED | F-003 |
| API-011 | GET | /api/businesses/{id}/documents | None | `ApiResponse<List<DocumentResponse>>` | ✅ IMPLEMENTED | F-003 |

### Knowledge Base & RAG (F-004)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-026 | POST | /api/businesses/{id}/knowledge-base/build | None | `ApiResponse<BuildResponse>` | ✅ IMPLEMENTED | F-004 (F-010/F-011) |
| API-027 | POST | /api/rag/search | `SearchRequest` | `ApiResponse<SearchResponse>` | ✅ IMPLEMENTED | F-004 (F-012/F-013) |

> **Business isolation**: `/api/rag/search` only ever returns chunks for the supplied
> `businessId`. Scoping is enforced in the service/repository layer (`findByBusinessId`),
> not on the client. A chunk from another business can never appear in results.

#### SearchRequest
```json
{
  "businessId": "uuid",
  "query": "what services do you offer?",
  "topK": 5
}
```
- `businessId` (required, UUID), `query` (required, non-empty).
- `topK` (optional, default 5, max 50). `limit` is accepted as an alias.

#### BuildResponse (`ApiResponse<BuildResponse>.data`)
```json
{
  "businessId": "uuid",
  "success": true,
  "status": "COMPLETED",
  "documentsProcessed": 5,
  "chunksCreated": 42,
  "embeddingsCreated": 42,
  "skipped": 0,
  "message": "Knowledge base built. 42 chunks created, 42 embeddings stored, 0 duplicates skipped."
}
```
- `status` ∈ `COMPLETED | NO_DOCUMENTS | EMBEDDING_NOT_CONFIGURED | EMBEDDING_FAILED | FAILED`.
- `404` if the business does not exist; `500` (clean `ApiResponse.error`) on build failure.

#### SearchResponse (`ApiResponse<SearchResponse>.data`)
```json
{
  "query": "what services do you offer?",
  "totalResults": 2,
  "items": [
    {
      "chunkId": "uuid",
      "content": "We offer ...",
      "sourceUrl": "https://example.com/services",
      "documentTitle": "Our Services",
      "rank": 1,
      "similarity": 0.87
    }
  ]
}
```
- `similarity` is a 0..1 cosine score (semantic mode) or a normalized keyword score
  (fallback when no embeddings exist); may be `null`.
- `400` for missing `businessId`/`query` or malformed UUID; `404` if business not found;
  `500` (clean `ApiResponse.error`) on embedding/vector-store failure.

### RAG Answer (F-004 / F-013)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-028 | POST | /api/rag/answer | `SearchRequest` (businessId, query, topK) | `ApiResponse<AnswerResponse>` | ✅ IMPLEMENTED | F-004 (F-013) |

Retrieves business-scoped top-K chunks, grounds the configured LLM ONLY on the cleaned
top chunks, and returns a concise answer + source URLs. The raw retrieved chunks are still
returned for debugging. No hallucination: if context is weak/empty the fixed fallback
sentence is returned WITHOUT calling the model.

#### AnswerResponse (`ApiResponse<AnswerResponse>.data`)
```json
{
  "businessId": "uuid",
  "query": "what is this business about",
  "answer": "This business offers ...",
  "sources": ["https://example.com/services", "https://example.com/ctv"],
  "results": [ /* same RagResultItem[] as /api/rag/search */ ],
  "topK": 5,
  "status": "COMPLETED"
}
```
- `status` ∈ `COMPLETED | WEAK_CONTEXT | NO_RESULTS | ANSWER_UNAVAILABLE | ANSWER_FAILED`.
- When `status` != `COMPLETED`, `answer` is the fixed sentence:
  `"I could not find enough knowledge base content to answer this confidently."`
- Same error codes as `/api/rag/search` (400/404/500). Business isolation is inherited.

### Support Chat (F-005)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-029 | POST | /api/chat/ask | `AskRequest` | `AskResponse` | ✅ IMPLEMENTED | F-005 |
| API-030 | GET | /api/chat/conversations/{conversationId}/messages | None | `ConversationHistoryResponse` | ✅ IMPLEMENTED | F-005 |

#### AskRequest
```json
{
  "businessId": 1,
  "conversationId": null,
  "question": "What services do you provide?"
}
```
- `businessId` (required, Long): The business ID to query against
- `conversationId` (optional, Long): Existing conversation ID. If `null`, creates new conversation
- `question` (required, non-empty string): The customer question

#### AskResponse
```json
{
  "conversationId": 1,
  "answer": "Based on our services page, we provide...",
  "sources": ["https://example.com/services", "https://example.com/about"],
  "confidenceScore": 85,
  "leadDetected": false,
  "leadId": null,
  "evaluation": {
    "hallucinationRisk": "LOW",
    "safeToSend": true,
    "reason": "Answer is supported by retrieved chunks."
  }
}
```
- `conversationId` (Long): The conversation ID (newly created or existing)
- `answer` (String): The AI-generated answer based on knowledge base (post-evaluation; replaced with the safe fallback if the draft was unsafe)
- `sources` (String[]): List of unique source URLs from retrieved chunks
- `confidenceScore` (int 0-100): Confidence percentage based on RAG similarity scores
- `leadDetected` (boolean, F-006): true when a lead was created during this turn
- `leadId` (UUID|null, F-006): the created lead's ID, if any
- `evaluation` (object|null, F-008): Evaluation Agent verdict; `null` for greetings / unevaluated turns
  - `hallucinationRisk` (String): `LOW` | `MEDIUM` | `HIGH`
  - `safeToSend` (boolean): whether the draft answer was safe to send
  - `reason` (String): short, customer-safe explanation (never exposes internal prompt/system details)

**Special Behavior**:
- Every RAG-generated draft answer is checked by the Evaluation Agent (F-008) before being sent.
- If the draft is unsafe (invented pricing/discount/guarantee, unsupported service, legal/refund
  claim, or empty context), the answer is replaced with:
  `"I do not have confirmed information about that. Please share your contact details and our team will help you."`
- Never invents pricing, promises discounts, or finalizes deals
- Always answers from retrieved knowledge chunks only

### Evaluation Agent (F-008)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-045 | POST | /api/evaluation/test | `EvaluationRequest` | `EvaluationResponse` | ✅ IMPLEMENTED | F-008 |

#### EvaluationRequest
```json
{
  "businessId": 1,
  "conversationId": null,
  "question": "What is your price?",
  "draftAnswer": "Our website starts from ₹10000.",
  "retrievedChunks": ["We provide website development and chatbot setup."],
  "sourceUrls": ["https://example.com/services"]
}
```
- `businessId` (required, UUID): business the answer belongs to
- `conversationId` (optional, UUID): conversation, if evaluated inside a chat
- `question` (String): the customer question
- `draftAnswer` (String): the Support Agent's draft answer to evaluate
- `retrievedChunks` (String[]): the retrieved knowledge chunks used as grounding
- `sourceUrls` (String[]): source URLs for the chunks

#### EvaluationResponse
```json
{
  "confidenceScore": 30,
  "hallucinationRisk": "HIGH",
  "safeToSend": false,
  "reason": "Answer mentioned pricing but pricing was not found in retrieved chunks.",
  "finalAnswer": "I do not have confirmed information about that. Please share your contact details and our team will help you."
}
```
- `confidenceScore` (int 0-100): confidence the answer is grounded
- `hallucinationRisk` (String): `LOW` | `MEDIUM` | `HIGH`
- `safeToSend` (boolean): true = keep original draft; false = use `finalAnswer`
- `reason` (String): short, clear reason
- `finalAnswer` (String|null): safe fallback answer when unsafe; `null` when safe

#### ConversationHistoryResponse
```json
{
  "conversationId": 1,
  "messages": [
    {
      "id": 1,
      "role": "USER",
      "content": "What services do you provide?",
      "createdAt": "2026-07-02T12:00:00Z"
    },
    {
      "id": 2,
      "role": "ASSISTANT",
      "content": "We provide...",
      "createdAt": "2026-07-02T12:00:05Z"
    }
  ]
}
```

### Agent Logs (F-012)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-065 | GET | /api/agent-logs | Query params (search, agentName, action, status, businessId, startDate, endDate, page, size, sort) | `PaginatedResponse<AgentLogResponse>` | ✅ IMPLEMENTED | F-012 |
| API-066 | GET | /api/agent-logs/{id} | None | `ApiResponse<AgentLogResponse>` | ✅ IMPLEMENTED | F-012 |
| API-067 | GET | /api/agent-logs/summary | None | `ApiResponse<AgentLogSummaryResponse>` | ✅ IMPLEMENTED | F-012 |

#### AgentLogResponse
```json
{
  "id": "uuid",
  "agentName": "RagSearch",
  "action": "SEARCH_COMPLETED",
  "status": "SUCCESS",
  "durationMs": 1250,
  "createdAt": "2026-07-05T12:00:00Z",
  "businessId": "uuid",
  "businessName": "Example Co",
  "leadId": "uuid",
  "leadName": "John Doe",
  "conversationId": "uuid",
  "inputJson": "{\"query\":\"pricing\"}",
  "outputJson": "{\"results\":[...]}",
  "errorMessage": null
}
```
- `id` (UUID): Execution ID
- `agentName` (String): Agent that executed the action
- `action` (String): Action type (e.g., SEARCH_COMPLETED, ANSWER_GENERATED)
- `status` (String): SUCCESS | PARTIAL | ERROR | FAILED | FALLBACK_USED
- `durationMs` (Long): Execution duration in milliseconds
- `createdAt` (LocalDateTime): When the execution occurred
- `businessId`, `businessName`, `leadId`, `leadName`, `conversationId` (optional): Related entities
- `inputJson` (String, optional): Execution input data (JSON or text)
- `outputJson` (String, optional): Execution output data (JSON or text)
- `errorMessage` (String, optional): Error details if execution failed

#### AgentLogSummaryResponse
```json
{
  "executionsToday": 142,
  "successRate": 94.5,
  "errorCount": 8,
  "averageDurationMs": 856
}
```
- `executionsToday` (long): Number of agent executions today
- `successRate` (double): Overall success rate percentage
- `errorCount` (long): Total number of errors (ERROR + FAILED status)
- `averageDurationMs` (Long): Average execution duration in milliseconds

### Chat (Future)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-013 | POST | /api/chat | `ChatRequest` | `ChatResponse` | ❌ NOT STARTED | F-014 |
| API-014 | GET | /api/conversations | Query params (businessId, status, page) | `PaginatedConversationResponse` | ❌ NOT STARTED | F-015 |
| API-015 | GET | /api/conversations/{id}/messages | Query params (page, size) | `PaginatedMessageResponse` | ❌ NOT STARTED | F-015 |

### Leads
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-014 | GET | /api/leads | Query params (businessId, status, score, page) | `PaginatedLeadResponse` | ❌ NOT STARTED | F-019 |
| API-015 | GET | /api/leads/{id} | None | `LeadResponse` | ❌ NOT STARTED | F-019 |
| API-016 | PUT | /api/leads/{id}/status | `UpdateLeadStatusRequest` | `LeadResponse` | ❌ NOT STARTED | F-019 |

### Follow-up & Approvals (F-007)
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-031 | POST | /api/leads/{leadId}/follow-up/generate | `FollowUpGenerateRequest` | `FollowUpGenerateResponse` | ✅ IMPLEMENTED | F-007 |
| API-032 | GET | /api/approvals | Query params (status?, type?, leadId?, businessId?) | `List<ApprovalResponse>` | ✅ IMPLEMENTED | F-007 |
| API-033 | GET | /api/approvals/{id} | None | `ApprovalResponse` | ✅ IMPLEMENTED | F-007 |
| API-034 | PUT | /api/approvals/{id}/approve | None | `ApprovalResponse` | ✅ IMPLEMENTED | F-007 |
| API-035 | PUT | /api/approvals/{id}/reject | None | `ApprovalResponse` | ✅ IMPLEMENTED | F-007 |
| API-036 | PUT | /api/approvals/{id}/status | `ApprovalStatusUpdateRequest` | `ApprovalResponse` | ✅ IMPLEMENTED | F-007 |

#### FollowUpGenerateRequest
```json
{
  "tone": "ALL"
}
```
- `tone` (optional, String): Message style - "PROFESSIONAL", "FRIENDLY", "SHORT_WHATSAPP", or "ALL" (default: "ALL" generates all 3 styles)

#### FollowUpGenerateResponse
```json
{
  "leadId": "uuid",
  "approvals": [
    {
      "approvalId": 1,
      "leadId": "uuid",
      "leadName": "John Doe",
      "businessId": "uuid",
      "businessName": "Example Co",
      "type": "FOLLOW_UP_MESSAGE",
      "status": "PENDING",
      "content": "Hi John, thank you for your interest...",
      "style": "PROFESSIONAL",
      "createdAt": "2026-07-02T12:00:00Z",
      "updatedAt": "2026-07-02T12:00:00Z"
    }
  ]
}
```

#### ApprovalResponse
```json
{
  "approvalId": 1,
  "leadId": "uuid",
  "leadName": "John Doe",
  "businessId": "uuid",
  "businessName": "Example Co",
  "type": "FOLLOW_UP_MESSAGE",
  "status": "PENDING",
  "content": "Hi John, thank you for your interest in our services...",
  "style": "PROFESSIONAL",
  "createdAt": "2026-07-02T12:00:00Z",
  "updatedAt": "2026-07-02T12:00:00Z"
}
```
- `status` ∈ `PENDING | APPROVED | REJECTED`
- `type` ∈ `FOLLOW_UP_MESSAGE | OUTBOUND_CALL | OUTREACH_MESSAGE`
- `style` (optional): Message style for FOLLOW_UP_MESSAGE type

#### ApprovalStatusUpdateRequest
```json
{
  "status": "APPROVED"
}
```

**API Notes:**
- POST /api/leads/{leadId}/follow-up/generate creates 3 approval records (one per style) when tone="ALL"
- All approvals start with status=PENDING
- Approve/reject actions update status and log to AgentLog
- No automatic message sending - approvals are for human review only
- GET /api/approvals supports filtering by status, type, leadId, businessId

### Voice Calls
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-021 | GET | /api/calls | Query params (leadId, status, page) | `PaginatedCallResponse` | ❌ NOT STARTED | F-022|
| API-022 | GET | /api/calls/{id} | None | `CallResponse` | ❌ NOT STARTED | F-022|

### Agent Logs
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-023 | GET | /api/agent-logs | Query params (agentType, action, status, startDate, endDate, page) | `PaginatedLogResponse` | ❌ NOT STARTED | F-025|

### Reports
| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID|
|--------|--------|----------|--------------|--------|-------------------|
| API-024 | POST | /api/reports/generate | `GenerateReportRequest` | `ReportResponse` | ❌ NOT STARTED | F-024|
| API-025 | GET | /api/reports/{id} | None | `ReportResponse` | ❌ NOT STARTED | F-024|

### Apify Lead Finder (F-010 / F-024)
All endpoints return the standard `ApiResponse<T>` wrapper `{ success, data, message }`.
The Apify API token is server-side only and is never exposed to the frontend.

| API ID | Method | Endpoint | Request Body | Response Body | Status | Related Feature ID |
|--------|--------|----------|--------------|---------------|--------|--------------------|
| API-026 | GET | /api/lead-finder/config | None | `{ apifyConfigured: boolean }` | ✅ DONE | F-024 |
| API-027 | POST | /api/lead-finder/runs | `StartLeadFinderRunRequest` | `LeadSourceRunResponse` | ✅ DONE | F-024 |
| API-028 | GET | /api/lead-finder/runs | None | `LeadSourceRunResponse[]` | ✅ DONE | F-024 |
| API-029 | GET | /api/lead-finder/runs/{id} | None | `LeadSourceRunResponse` | ✅ DONE | F-024 |
| API-030 | GET | /api/lead-finder/runs/{id}/results | None | `DiscoveredLeadResponse[]` | ✅ DONE | F-024 |
| API-031 | POST | /api/lead-finder/runs/{id}/sync | None | `LeadSourceRunResponse` | ✅ DONE | F-024 |
| API-032 | POST | /api/lead-finder/discovered-leads/{id}/import | `ImportDiscoveredLeadRequest` (optional `targetBusinessId`) | `DiscoveredLeadResponse` | ✅ DONE | F-024 |
| API-033 | POST | /api/lead-finder/discovered-leads/import-bulk | `BulkImportDiscoveredLeadsRequest` | `BulkImportResultResponse` | ✅ DONE | F-024 |
| API-034 | POST | /api/lead-finder/discovered-leads/{id}/reject | None | `DiscoveredLeadResponse` | ✅ DONE | F-024 |

**Behavioural notes**
- When Apify is not configured (`APIFY_ENABLED=false` or missing token), `POST /runs` and
  `/runs/{id}/sync` return HTTP **503** with `{ success:false, message:"Apify is not configured." }`.
  The app still starts and all other endpoints keep working.
- `POST /discovered-leads/{id}/import` returns HTTP **409** when the lead would duplicate an
  existing CRM lead (matched by email → phone → website → business name + location).
- Lead requires a business, so import needs a `targetBusinessId`. If omitted and exactly one
  business exists it is auto-selected; otherwise a 400 asks the admin to pick one.

**StartLeadFinderRunRequest**
```json
{
  "searchName": "Hyderabad ad agencies",
  "industry": "Advertising agencies",
  "location": "Hyderabad",
  "keywords": "media buying",
  "actorId": null,
  "maxResults": 25
}
```

**DiscoveredLeadResponse**
```json
{
  "id": "uuid",
  "leadSourceRunId": "uuid",
  "businessName": "Acme Media",
  "websiteUrl": "https://acme.example",
  "contactName": "Jane Doe",
  "email": "jane@acme.example",
  "phone": "+9199...",
  "location": "Hyderabad",
  "industry": "Advertising",
  "sourceUrl": "https://maps.google/...",
  "score": 85.0,
  "status": "NEW",
  "importedLeadId": null,
  "createdAt": "2026-07-03T12:00:00Z",
  "updatedAt": "2026-07-03T12:00:00Z"
}
```

---

**Last Updated**: 2026-07-03