# AgentOps CRM - Production Security Audit & Implementation Plan

**Date**: 2026-07-05  
**Project Path**: `/Users/jaya/Desktop/crm`  
**Task**: Complete production security hardening with single-ADMIN authentication

---

## EXECUTIVE SUMMARY

This document provides a comprehensive audit of the current AgentOps CRM implementation and a detailed plan to harden the system for production deployment with single-role ADMIN authentication.

### Critical Findings

🔴 **CRITICAL** - No authentication implemented  
🔴 **CRITICAL** - All APIs publicly accessible  
🔴 **CRITICAL** - Production secrets exposed in `.env` file  
🔴 **CRITICAL** - Git not initialized - no version control  
🔴 **CRITICAL** - `ddl-auto: update` in base config (unsafe for production)  
🔴 **CRITICAL** - Flyway disabled (`enabled: false`)  
🟡 **WARNING** - Frontend build fails (TypeScript errors in Conversations.tsx)  
🟡 **WARNING** - PostgreSQL text-based vector storage (not pgvector)  
🟡 **WARNING** - Open CORS configuration (localhost only, but should be environment-based)  
🟢 **GOOD** - Backend compiles successfully  
🟢 **GOOD** - 9 Database migrations exist and are well-structured  
🟢 **GOOD** - Comprehensive feature implementation (Settings, Agent Logs, Voice Calls, etc.)

---

## 1. CURRENT STATE AUDIT

### 1.1 Git Repository Status

**Status**: ❌ NOT INITIALIZED

```bash
$ git status
fatal: not a git repository
```

**Risk**: No version control, no ability to roll back changes, hard to collaborate

**`.gitignore` exists**: ✅ YES (comprehensive, covers .env, node_modules, dist, target, etc.)

### 1.2 Frontend Build Status

**Status**: ❌ FAILING

```
src/pages/Conversations.tsx(18,3): error TS6196: 'Channel' is declared but never used.
src/pages/Conversations.tsx(19,3): error TS6196: 'MessageRole' is declared but never used.
src/pages/Conversations.tsx(37,10): error TS6133: 'loadingDetail' is declared but its value is never read.
```

**Confirmed**: These match the exact errors mentioned in the task requirements.

### 1.3 Backend Status

**Compilation**: ✅ SUCCESSFUL (backend is running)  
**Spring Security**: ❌ NOT CONFIGURED (`backend/src/main/java/com/agentopscrm/security/` is empty)  
**Authentication**: ❌ NONE - All endpoints publicly accessible

### 1.4 Database Configuration Analysis

**File**: [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml:1)

**Current Base Configuration**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update    # ❌ UNSAFE FOR PRODUCTION
  flyway:
    enabled: false        # ❌ MIGRATIONS DISABLED
```

**Production Config** ([`application-prod.yml`](backend/src/main/resources/application-prod.yml:1)): Exists but details need review

**Risk**: `ddl-auto: update` can cause data loss. Flyway disabled means migrations not enforced.

### 1.5 Secrets in Environment Files

**File**: [`.env`](/.env:1)

**CRITICAL SECURITY ISSUE**: Contains production API keys:
- OPENAI_API_KEY: `sk-proj-....` (real key exposed)
- FIRECRAWL_API_KEY: `fc-663f...` (real key exposed)

**Status**: ❌ These keys should be rotated immediately after implementing proper secret management

### 1.6 CORS Configuration

**File**: [`backend/src/main/java/com/agentopscrm/config/CorsConfig.java`](backend/src/main/java/com/agentopscrm/config/CorsConfig.java:1)

**Current**: Centralized CORS filter (✅ GOOD)
**Allowed Origins**: Hardcoded localhost:5173, 127.0.0.1:5173, localhost:3000, 127.0.0.1:3000  
**Issue**: Should be environment-configurable for production  
**Credentials**: ✅ `allowCredentials: true` (supports cookie-based auth)

**Controller-level CORS**: Need to scan for any `@CrossOrigin(origins = "*")` annotations

### 1.7 Existing Database Migrations

**Location**: `backend/src/main/resources/migration/`

**Migrations**:
1. V1__create_tables.sql - Base schema
2. V2__add_knowledge_chunk_embedding.sql - Knowledge base
3. V3__add_lead_capture_fields.sql - Lead capture
4. V4__add_approval_style.sql -Approval styling
5. V5__add_voice_call_fields.sql - Voice features
6. V6__fix_voice_call_status_constraint.sql - Status fix
7. V7__add_lead_finder_tables.sql - Lead discovery
8. V8__fix_agent_logs_status_constraint.sql - Agent logs
9. V9__add_conversations_updated_at_index.sql - Performance

**Status**: ✅ Well-structured, incremental, follows best practices

**Missing**: No `admins` or `admin_users` table for authentication

### 1.8 Features Implemented (from reports)

✅ **F-012**: Agent Logs (Complete observability)  
✅ **F-013**: Settings Page (Production-ready diagnostics)  
✅ Conversations Admin Page  
✅ Voice Calls (Vapi integration)  
✅ Lead Finder (Apify integration)  
✅ Knowledge Base & RAG  
✅ Approvals System  
✅ Businesses, Leads, Dashboard

**Note**: All these features are UNPROTECTED and publicly accessible

### 1.9 Vector Storage

**Current**: `postgres-text` strategy (embeddings stored as TEXT, ranked in-memory)  
**Planned**: pgvector extension (database-side similarity search)  
**Status**: ⚠️ Text-based storage confirmed in [application.yml:99](backend/src/main/resources/application.yml:99)

---

## 2. IMPLEMENTATION PLAN

### Phase 1: Fix Frontend Build ✅ HIGHEST PRIORITY
**Time**: 10 minutes

**Tasks**:
1. Remove unused import [`Channel`](frontend/src/pages/Conversations.tsx:18) from Conversations.tsx
2. Remove unused import [`MessageRole`](frontend/src/pages/Conversations.tsx:19) from Conversations.tsx
3. Remove or use [`loadingDetail`](frontend/src/pages/Conversations.tsx:37) state
4. Run `npm run build` to confirm success
5. Do NOT disable strict TypeScript checks
6. Do NOT weaken tsconfig.json

### Phase 2: Git Safety & Initial Commit ✅ HIGH PRIORITY
**Time**: 15 minutes

**Tasks**:
1. Initialize Git: `git init`
2. Review [`.gitignore`](./.gitignore:1) - already comprehensive ✅
3. Scan for secrets in tracked files (already identified in .env)
4. **DO NOT commit .env** - it's already in .gitignore ✅
5. Create initial commit with message: "chore: initial commit - pre-authentication baseline"
6. **DO NOT** create GitHub Actions, GitLab CI, or any CI/CD
7. **DO NOT** configure remote
8. **DO NOT** push to any remote

### Phase 3: Admin Bootstrap Environment Safety ✅ HIGH PRIORITY
**Time**: 20 minutes

**Tasks**:
1. Add to .env:
   ```
   ADMIN_EMAIL=admin@agentops.local
   ADMIN_PASSWORD=<generate-secure-random>
   ```
2. Document in ENVIRONMENT.md:
   - Never use default passwords in production
   - Rotate bootstrap credentials after first login
   - Remove from .env after admin creation
3. Add validation: fail startup if production & credentials missing
4. Never log passwords

### Phase 4: Backend - Admin Entity & Repository
**Time**: 30 minutes

**Files to Create**:
1. `backend/src/main/java/com/agentopscrm/entity/Admin.java`
   - UUID id
   - String email (unique, indexed)
   - String passwordHash (BCrypt/Argon2)
   - LocalDateTime createdAt, lastLoginAt
   - boolean enabled

2. `backend/src/main/java/com/agentopscrm/repository/AdminRepository.java`
   - `Optional<Admin> findByEmail(String email)`
   - `boolean existsByEmail(String email)`

3. Database Migration: `V10__create_admins_table.sql`
   ```sql
   CREATE TABLE admins (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       email VARCHAR(255) NOT NULL UNIQUE,
       password_hash VARCHAR(255) NOT NULL,
       enabled BOOLEAN NOT NULL DEFAULT true,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       last_login_at TIMESTAMP,
       CONSTRAINT admins_email_key UNIQUE (email)
   );
   CREATE INDEX idx_admins_email ON admins(email);
   ```

### Phase 5: Spring Security Configuration
**Time**: 1 hour

**Files to Create**:

1. `backend/pom.xml` - Add dependency:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-security</artifactId>
   </dependency>
   ```

2. `backend/src/main/java/com/agentopscrm/security/SecurityConfig.java`
   - HttpSecurity configuration
   - Permit: `/api/auth/login`, `/api/health`, `/api/vapi/webhook`
   - Require authentication: Everything else
   - CSRF configuration (appropriate for cookie-based auth)
   - Session management
   - BCryptPasswordEncoder bean

3. `backend/src/main/java/com/agentopscrm/security/AdminUserDetailsService.java`
   - Implements UserDetailsService
   - Loads admin by email
   - Maps to Spring Security UserDetails

4. `backend/src/main/java/com/agentopscrm/security/AdminAuthenticationSuccessHandler.java`
   - Update last_login_at
   - Log successful login (no password logging)

5. `backend/src/main/java/com/agentopscrm/security/AdminAuthenticationFailureHandler.java`
   - Log failed attempts
   - No password logging
   - Safe error messages

### Phase 6: Authentication Service & Controller
**Time**: 45 minutes

**Files to Create**:

1. `backend/src/main/java/com/agentopscrm/service/AdminBootstrapService.java`
   - @PostConstruct or CommandLineRunner
   - Check if admin exists
   - If not, create from environment variables
   - Hash password with BCrypt
   - Never recreate if exists
   - Fail safely in production if credentials missing

2. `backend/src/main/java/com/agentopscrm/service/AuthService.java`
   - Login logic
   - Password verification
   - Session management

3. `backend/src/main/java/com/agentopscrm/controller/AuthController.java`
   - POST /api/auth/login
   - GET /api/auth/me (current user)
   - POST /api/auth/logout
   - Request validation
   - Rate limiting (later phase)

4. DTOs:
   - `LoginRequest` (email, password)
   - `LoginResponse` (success, message)
   - `AdminResponse` (id, email, no password)

### Phase 7: Frontend Authentication
**Time**: 1 hour

**Files to Create**:

1. `frontend/src/types/auth.ts`
   - Admin interface
   - LoginRequest, LoginResponse types

2. `frontend/src/api/authApi.ts`
   - login(email, password)
   - getCurrentAdmin()
   - logout()

3. `frontend/src/context/AuthContext.tsx`
   - AuthProvider with admin state
   - Login/logout functions
   - Session restoration on mount
   - useAuth hook

4. `frontend/src/components/auth/ProtectedRoute.tsx`
   - Wrapper for authenticated routes
   - Redirect to /login if not authenticated

5. `frontend/src/pages/Login.tsx`
   - Email & password inputs
   - Form validation
   - Error handling
   - Redirect after login
   - Dark glassmorphism design

6. `frontend/src/App.tsx` - Updates:
   - Wrap with AuthProvider
   - Wrap all routes except /login with ProtectedRoute
   - Add /login route
   - Axios 401 interceptor → redirect to /login

### Phase 8: Production Database Configuration
**Time**: 20 minutes

**Files to Modify**:

1. [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml:1)
   - Keep `ddl-auto: update` in base (dev convenience)

2. [`backend/src/main/resources/application-prod.yml`](backend/src/main/resources/application-prod.yml:1)
   - Set `spring.jpa.hibernate.ddl-auto: validate`  
   - Set `spring.flyway.enabled: true`
   - Set `spring.jpa.show-sql: false`
   - Set `spring.jpa.open-in-view: false`
   - Require environment variables for DB password
   - Configure connection pool limits

3. Test Flyway migrations:
   ```bash
   # Create empty test database
   # Run migrations
   # Verify schema
   ```

### Phase 9: CORS Hardening
**Time**: 15 minutes

**File**: [`backend/src/main/java/com/agentopscrm/config/CorsConfig.java`](backend/src/main/java/com/agentopscrm/config/CorsConfig.java:1)

**Changes**:
- Read allowed origins from environment variable: `CORS_ALLOWED_ORIGINS`
- Default dev: `http://localhost:5173,http://127.0.0.1:5173`
- Production: Must be explicitly set
- No wildcard with credentials

**Scan for controller-level CORS**:
```bash
grep -r "@CrossOrigin" backend/src/main/java/
```
Remove any unrestricted CORS annotations.

### Phase 10: Data Authorization & IDOR Prevention
**Time**: 2 hours

**Services to Update** (add relationship validation):

1. BusinessService - validate business ownership
2. LeadService - validate lead belongs to business
3. ConversationService - validate conversation relationships
4. VoiceCallService - validate call relationships
5. ApprovalService - validate approval relationships
6. AgentLogService - validate agent log relationships
7. KnowledgeBaseService - validate business-scoped retrieval

**Pattern**: Add helper methods in services:
```java
private void validateBusinessAccess(UUID businessId) {
    // In single-ADMIN mode, just verify business exists
    // Future: Check admin has access to this business
    if (!businessRepository.existsById(businessId)) {
        throw new BusinessNotFoundException(businessId);
    }
}

private void validateLeadBelongsToBusiness(UUID leadId, UUID businessId) {
    Lead lead = leadRepository.findById(leadId)
        .orElseThrow(() -> new LeadNotFoundException(leadId));
    if (!lead.getBusinessId().equals(businessId)) {
        throw new UnauthorizedException("Lead does not belong to specified business");
    }
}
```

### Phase 11: Rate Limiting
**Time**: 1.5 hours

**Implementation**: Bucket4j + Redis or in-memory (for simplicity)

**Endpoints to Limit**:
- POST /api/auth/login - 5 attempts per 15 minutes per IP
- POST /api/chat - 20 requests per minute per admin
- POST /api/rag/answer - 20 requests per minute per admin
- POST /api/evaluation - 10 requests per minute per admin
- POST /api/crawl - 5 requests per minute per admin
- POST /api/knowledge/build - 3 requests per minute per admin
- POST /api/lead-qualifier - 10 requests per minute per admin
- POST /api/lead-finder/start - 5 requests per minute per admin
- POST /api/voice-calls - 10 requests per minute per admin
- POST /api/settings/integrations/*/test - 3 requests per minute per admin

**Files to Create**:
1. `backend/src/main/java/com/agentopscrm/config/RateLimitConfig.java`
2. `backend/src/main/java/com/agentopscrm/filter/RateLimitFilter.java`
3. HTTP 429 response with Retry-After header

### Phase 12: Web Security Headers
**Time**: 30 minutes

**File**: `backend/src/main/java/com/agentopscrm/config/SecurityHeadersConfig.java`

**Headers to Add**:
- `Content-Security-Policy: default-src 'self'` (adjusted for inline styles)
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-Frame-Options: DENY`
- `Cache-Control: no-store` for sensitive endpoints

### Phase 13: Settings Page Protection & Corrections
**Time**: 1 hour

**Backend**: [`SettingsController`](backend/src/main/java/com/agentopscrm/controller/SettingsController.java:1)
- Add `@PreAuthorize("hasRole('ADMIN')") ` or equivalent
- Review diagnostic tests (already mostly safe per F-013 report)

**Corrections** (if not already fixed):
- Distinguish CONFIGURED from HEALTHY
- Real Redis connection test
- Real OpenAI key validation
- Correct Vapi webhook URL display
- Efficient aggregate queries in knowledge metrics

### Phase 14: Agent Logs Corrections
**Time**: 30 minutes

**Backend**: [`AgentLogService`](backend/src/main/java/com/agentopscrm/service/AgentLogService.java:1)

**Corrections**:
- Validate sort field allowlist
- Efficient summary queries (avoid loading 1000 rows)
- Add redaction service for sensitive fields
- Redact: API keys, authorization headers, passwords, webhook secrets

**Files to Create**:
1. `backend/src/main/java/com/agentopscrm/util/RedactionService.java`
   - Redact patterns: "api_key", "authorization", "password", "secret", "token"
   - Safe for JSON and plain text

### Phase 15: pgvector Migration
**Time**: 2 hours

**Database Migration**: `V11__enable_pgvector.sql`
```sql
CREATE EXTENSION IF NOT EXISTS vector;
ALTER TABLE knowledge_chunks ADD COLUMN embedding_vector vector(1536);
-- Migration script to convert TEXT embeddings to vector type
-- Add index: CREATE INDEX ON knowledge_chunks USING ivfflat (embedding_vector vector_cosine_ops);
```

**Backend Updates**:
1. Update [`KnowledgeChunk.java`](backend/src/main/java/com/agentopscrm/entity/KnowledgeChunk.java:1)
   - Add `@Column` for vector type
2. Update [`VectorStoreService`](backend/src/main/java/com/agentopscrm/service/VectorStoreService.java:1)
   - Database-side similarity search
   - Business-scoped queries
3. Update [`application.yml`](backend/src/main/resources/application.yml:99)
   - Set `rag.vector-store: pgvector`
4. Add tests: prevent cross-business retrieval

### Phase 16: Idempotency & Webhook Safety
**Time**: 1.5 hours

**Vapi Webhook**: [`VapiWebhookController`](backend/src/main/java/com/agentopscrm/controller/VapiWebhookController.java:1)
- Add signature validation (constant-time comparison)
- Store provider event IDs
- Reject duplicates
- Add tests

**Idempotency Keys**:
- Voice call creation
- Crawl job initiation
- Lead Finder runs
- Lead imports

**Files**:
1. `backend/src/main/java/com/agentopscrm/entity/IdempotencyKey.java`
2. `backend/src/main/java/com/agentopscrm/repository/IdempotencyKeyRepository.java`
3. Migration: `V12__create_idempotency_keys.sql`

### Phase 17: Testing
**Time**: 3 hours

**Backend Tests to Add** (JUnit + Mockito):

**Authentication Tests**:
- AdminBootstrapServiceTest
- AuthServiceTest (login, password verification)
- AuthControllerTest (endpoints)
- SecurityConfigTest (protected/public endpoints)

**Settings Tests**:
- SettingsServiceTest (secret sanitization, diagnostics)

**Authorization Tests**:
- BusinessService IDOR tests
- LeadService relationship validation
- ConversationService authorization

**Rate Limiting Tests**:
- Login rate limit
- API rate limits

**Frontend Tests** (Vitest + React Testing Library):
- Login component
- AuthContext
- ProtectedRoute
- Session restoration
- 401 handling

**Run**:
```bash
cd backend && mvn test
cd frontend && npm test
```

### Phase 18: Documentation Updates
**Time**: 1 hour

**Files to Update**:
1. `README.md` - Add authentication instructions, bootstrap process
2. `docs/ENVIRONMENT.md` - Document ALL environment variables
3. `docs/API_CONTRACT.md` - Add auth endpoints, update security notes
4. `docs/ROADMAP.md` - Mark authentication as COMPLETE
5. `docs/CHANGELOG.md` - Add entries for all security changes
6. `docs/DECISIONS.md` - Document authentication architecture choices
7. Create `docs/SECURITY.md` - Security model, threat mitigations, deployment checklist
8. Create `docs/BACKUP_AND_RECOVERY.md` - Backup procedures, retention policies

### Phase 19: Final Verification
**Time**: 1 hour

**Backend Verification**:
```bash
mvn clean test
mvn clean package
# Test with empty PostgreSQL database
# Test Flyway migrations
```

**Frontend Verification**:
```bash
npm run build
npm test
# TypeScript strict check
```

**Manual Browser Testing**:
- Login flow
- Session restoration
- Logout
- Protected route access while unauthenticated → redirects to login
- 401 handling
- All pages load correctly after login
- Dashboard, Businesses, Leads, Conversations, Voice Calls, Approvals, Agent Logs, Settings

**Security Testing**:
- Unauthenticated API requests → 401
- Invalid credentials → proper error
- SQL injection attempts (parameterized queries)
- CSRF token validation
- CORS preflight
- Rate limiting triggers 429
- Oversized requests rejected
- Invalid UUID/enum handling
- IDOR attempts blocked

---

## 3. OUT OF SCOPE (Per Requirements)

❌ **NOT Implementing**:
- Multiple roles (SALES, SUPPORT, VIEWER)
- Role-based permission matrices
- CI/CD pipelines (GitHub Actions, GitLab CI)
- Deployment automation
- Container registry publishing
- Background job queues (Redis durability for jobs)
- Data retention cleanup jobs (documentation only)
- Cloud infrastructure configuration

---

##4. SECURITY RISKS & MITIGATIONS

### Risk 1: Exposed Secrets in .env
**Severity**: CRITICAL  
**Mitigation**: 
1. Rotate keys immediately after implementation
2. Use secret management service in production
3. Never commit .env
4. Document rotation procedures

### Risk 2: Single Admin Account
**Severity**: MEDIUM  
**Mitigation**:
1. Strong password requirements
2. Rate limiting on login
3. Document password recovery process
4. Future: Add multi-admin support

### Risk 3: Session Hijacking
**Severity**: MEDIUM  
**Mitigation**:
1. HttpOnly cookies
2. Secure flag in production
3. SameSite=Strict
4. Short session timeout
5. CSRF protection

### Risk 4: IDOR Vulnerabilities
**Severity**: HIGH  
**Mitigation**:
1. Validate all resource relationships in service layer
2. Never trust client-provided IDs
3. Test authorization boundaries
4. Add integration tests for IDOR scenarios

---

## 5. ROLLBACK STRATEGY

**Before Starting**:
1. Create initial Git commit (baseline)
2. Tag as `pre-authentication-baseline`

**If Issues Occur**:
1. `git reset --hard pre-authentication-baseline`
2. Review logs
3. Fix issues
4. Resume from failed phase

**Database Rollback**:
- Flyway supports rollback migrations (if needed)
- Keep database backup before migration

---

## 6. ESTIMATED TIMELINES

| Phase | Description | Time | Priority |
|-------|-------------|------|----------|
| 1 | Fix Frontend Build | 10 min | CRITICAL |
| 2 | Git Safety | 15 min | CRITICAL |
| 3 | Admin Bootstrap Env | 20 min | CRITICAL |
| 4 | Admin Entity | 30 min | HIGH |
| 5 | Spring Security | 1 hour | HIGH |
| 6 | Auth Service & Controller | 45 min | HIGH |
| 7 | Frontend Auth | 1 hour | HIGH |
| 8 | Production DB Config | 20 min | HIGH |
| 9 | CORS Hardening | 15 min | MEDIUM |
| 10 | Data Authorization | 2 hours | HIGH |
| 11 | Rate Limiting | 1.5 hours | MEDIUM |
| 12 | Security Headers | 30 min | MEDIUM |
| 13 | Settings Protection | 1 hour | MEDIUM |
| 14 | Agent Logs Corrections | 30 min | MEDIUM |
| 15 | pgvector Migration | 2 hours | LOW |
| 16 | Idempotency | 1.5 hours | MEDIUM |
| 17 | Testing | 3 hours | HIGH |
| 18 | Documentation | 1 hour | HIGH |
| 19 | Verification | 1 hour | CRITICAL |

**Total Estimated Time**: ~18 hours (2-3 days of focused work)

**MVP (Minimum Viable Product)**: Phases 1-8 (~4 hours) - Gets authentication working
**Full Production-Ready**: All 19 phases (~18 hours)

---

## 7. SUCCESS CRITERIA

✅ Frontend builds successfully without TypeScript errors  
✅ Git initialized with safe .gitignore  
✅ No secrets committed  
✅ Admin account can be created via environment variables  
✅ Login/logout working  
✅ All pages protected except login  
✅ Session restoration working  
✅ 401 handling redirects to login  
✅ CORS configured for production  
✅ Database configured safely for production  
✅ Flyway migrations execute successfully  
✅ Rate limiting functional on sensitive endpoints  
✅ IDOR attempts blocked  
✅ Tests pass (backend + frontend)  
✅ Documentation complete and accurate  
✅ Manual browser verification successful  
✅ Security testing passed  

---

## 8. NEXT STEPS

**Immediate Actions**:
1. Present this plan for review/approval
2. Obtain approval to proceed with implementation
3. Start with Phase 1 (Frontend Build Fix)
4. Proceed sequentially through phases
5. Test thoroughly after each phase
6. Document any deviations from plan

**Questions for Stakeholders**:
1. Approve exposed secret rotation timeline?
2. Preferred password hashing: BCrypt or Argon2?
3. Session timeout duration (e.g., 8 hours, 24 hours)?
4. Rate limit thresholds acceptable?
5. Production deployment timeline?

---

**Document Status**: READY FOR REVIEW  
**Author**: Senior Full-Stack Engineer (AI Assistant)  
**Approval Required**: YES - Before proceeding with implementation
