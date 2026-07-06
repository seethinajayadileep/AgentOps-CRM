# AgentOps CRM - Production Deployment Guide

**Last Updated**: 2026-07-06  
**Status**: 🚧 IN PROGRESS - NOT READY FOR DEPLOYMENT  
**Version**: 1.0.0

---

## 🚨 CRITICAL: Stop and Read First

**DO NOT DEPLOY until all items in this checklist are completed AND all exposed credentials are rotated.**

### Pre-Deployment Security Checklist

- [ ] ✅ **Apify API Token** - Rotated (see SECURITY_AUDIT_CRITICAL.md)
- [ ] ✅ **OpenAI API Key** - Rotated
- [ ] ✅ **Firecrawl API Key** - Rotated  
- [ ] **Vapi Keys** - Verified not exposed
- [ ] **JWT Secret** - Strong random generated for production
- [ ] **Admin Password** - Bcrypt hashed
- [ ] All `.env` files removed from git tracking
- [ ] No secrets in logs or documentation

⚠️ **See [`SECURITY_AUDIT_CRITICAL.md`](SECURITY_AUDIT_CRITICAL.md) for detailed rotation instructions.**

---

## Architecture Overview

```
┌─────────────────┐         ┌──────────────────┐
│  Vercel         │────────▶│  Railway         │
│  (Frontend)     │  HTTPS  │  (Backend API)   │
│  React + Vite   │         │  Spring Boot     │
└─────────────────┘         └────────┬─────────┘
                                     │
                         ┌───────────┴──────────┐
                         │                      │
                  ┌──────▼──────┐      ┌───────▼──────┐
                  │  PostgreSQL  │      │    Redis     │
                  │  + pgvector  │      │  (optional)  │
                  └──────────────┘      └──────────────┘
```

---

## Step 1: Rotate Exposed Credentials ✅

**STATUS**: Audit Complete - Rotation Required

### Exposed Secrets Identified

1. **Apify Token** (CRITICAL):
   - Location: Was in `backend/.env.example` line 44
   - Action: ✅ Sanitized with placeholder
   - **YOU MUST**: Rot

ate at https://console.apify.com/account/integrations

2. **OpenAI Key** (CRITICAL):
   - Location: `.env` and `backend/.env` (not tracked in git ✅)
   - **YOU MUST**: Rotate at https://platform.openai.com/api-keys

3. **Firecrawl Key** (HIGH):
   - Location: `.env` and `backend/.env` (not tracked in git ✅)
   - **YOU MUST**: Rotate at https://firecrawl.dev/dashboard

### How to Rotate

1. Visit the service dashboard
2. Revoke/delete the old key
3. Generate a new key
4. Update your local `.env` and `backend/.env` files (DO NOT COMMIT)
5. Configure Railway environment variables with new keys

---

## Step 2: Backend Configuration ✅

### Spring Profiles Fixed

**Changes Made**:
- ❌ Removed unsafe default: `spring.profiles.active: dev`
- ✅ Now requires explicit `SPRING_PROFILES_ACTIVE` environment variable
- ✅ Railway PORT support: `server.port: ${PORT:8080}`
- ✅ Safe defaults: `ddl-auto: none`, `show-sql: false`, logging: INFO
- ✅ Production Hikari pool: 5 max connections (optimal for Railway)
- ✅ Removed `baseline-on-migrate: true` from production
- ✅ Added graceful shutdown and compression

### Files Modified:
- [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)
- [`backend/src/main/resources/application-dev.yml`](backend/src/main/resources/application-dev.yml)
- [`backend/src/main/resources/application-prod.yml`](backend/src/main/resources/application-prod.yml)

---

## Step 3: Railway PORT Support ✅

**Status**: Complete

The backend now listens on Railway's PORT environment variable:

```yaml
server:
  port: ${PORT:8080}
  address: 0.0.0.0
```

This allows Railway to dynamically assign ports.

---

## Step 4: PostgreSQL + pgvector Setup 🔄

### Railway PostgreSQL Configuration

1. **Provision Railway PostgreSQL**:
   - Create new PostgreSQL service in Railway
   - Railway will provide connection variables

2. **Enable pgvector Extension**:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
   - Flyway migration V10 will handle this automatically
   - Ensure Railway PostgreSQL supports pgvector

3. **Connection Variables**:
   Railway provides:
   - `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
   
   Configure as:
   ```bash
   DB_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
   DB_USER=${PGUSER}
   DB_PASSWORD=${PGPASSWORD}
   DB_POOL_MAX=5
   DB_POOL_MIN=1
   ```

4. **Private Networking**:
   - Use Railway's private networking for backend ↔ database
   - More secure and lower latency

---

## Step 5: Flyway pgvector Migration ✅

**Status**: Fixed

### Issue
`CREATE INDEX CONCURRENTLY` cannot run inside a Flyway transaction.

### Solution
Created [`FlywayConfig.java`](backend/src/main/java/com/agentopscrm/config/FlywayConfig.java) that enables mixed-mode migrations.

### Verification
On first deployment to empty Railway PostgreSQL:
1. Flyway runs all migrations from V1
2. V10 creates pgvector extension
3. V10 adds `embedding_vector` column
4. V10 creates ivfflat index (CONCURRENTLY, outside transaction)

---

## Step 6: Redis Configuration 🔄 TODO

**Determine if Redis is actually required.**

Currently configured but may be optional for the application.

### Investigation Needed:
- Check if any services actually use Redis
- Review Spring Data Redis dependencies
- Test application without Redis

### Options:
1. **If NOT Required**:
   - Make Redis optional/disabled
   - Disable Redis health check in production
   
2. **If Required**:
   - Provision Railway Redis
   - Configure with Railway variables
   - Enable health check

---

## Step 7: Production CORS 🔄 TODO

**Current Issue**: CORS only allows localhost

### Requirements:
```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

### Production Variable:
```bash
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,https://your-custom-domain.com
```

### Implementation TODO:
- Update CORS configuration class
- Support comma-separated origins
- Handle Vercel preview deployments safely
- Do NOT use wildcard `*` with credentials

---

## Step 8: Frontend API Configuration 🔄 TODO

**Current Issue**: Multiple hardcoded `localhost:8080` references

### Files to Fix:
- `frontend/src/api/chat.ts`
- `frontend/src/api/approvalsApi.ts`
- `frontend/src/api/leadsApi.ts`
- Other API files

### Solution:
1. Create single Axios instance: `frontend/src/api/axios.ts`
2. Use one environment variable: `VITE_API_BASE_URL`
3. Remove hardcoded URLs
4. Add build-time validation (fail if localhost in production build)

### Vercel Environment Variable:
```bash
VITE_API_BASE_URL=https://your-backend.up.railway.app/api
```

---

## Step 9: Vercel Configuration 🔄 TODO

### Create `frontend/vercel.json`:
```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

### Vercel Project Settings:
- **Root Directory**: `frontend`
- **Framework**: Vite
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm ci`

### Environment Variables:
```bash
VITE_API_BASE_URL=https://YOUR-BACKEND.up.railway.app/api
```

---

## Step 10: Railway Backend Dockerfile 🔄 TODO

Create `backend/Dockerfile` with:
- Java 21 base image
- Multi-stage build
- Maven build stage
- Test execution
- Small JRE runtime
- Non-root user
- Updated CA certificates for Apify TLS
- Health check
- Graceful shutdown

---

## Step 11: Railway Deployment Config 🔄 TODO

Create `backend/railway.toml`:
```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"

[deploy]
startCommand = ""
healthcheckPath = "/actuator/health"
healthcheckTimeout = 100
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 10
```

---

## Step 12: Environment Variables Checklist 🔄 TODO

### Railway Backend Variables:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database (from Railway PostgreSQL service)
DB_URL=jdbc:postgresql://${Postgres.PGHOST}:${Postgres.PGPORT}/${Postgres.PGDATABASE}
DB_USER=${Postgres.PGUSER}
DB_PASSWORD=${Postgres.PGPASSWORD}
DB_POOL_MAX=5
DB_POOL_MIN=1

# CORS
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app

# API Keys (MUST BE ROTATED)
OPENAI_API_KEY=sk-your-new-key-here
FIRECRAWL_API_KEY=fc-your-new-key-here

# Apify
APIFY_ENABLED=true
APIFY_API_TOKEN=apify_api_YOUR_NEW_TOKEN_HERE
APIFY_DEFAULT_ACTOR_ID=compass~crawler-google-places

# Vapi (if used)
VAPI_ENABLED=true
VAPI_API_KEY=your-vapi-key
VAPI_ASSISTANT_ID=your-assistant-id
VAPI_PHONE_NUMBER_ID=your-phone-uuid
VAPI_WEBHOOK_SECRET=your-webhook-secret

# Application
APP_PUBLIC_BASE_URL=https://your-backend.up.railway.app

# Security
JWT_SECRET=<generate-with-openssl-rand-base64-64>
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH=<bcrypt-hash>

# Redis (if used)
REDIS_HOST=${Redis.REDIS_HOST}
REDIS_PORT=${Redis.REDIS_PORT}
REDIS_PASSWORD=${Redis.REDIS_PASSWORD}
REDIS_HEALTH_ENABLED=true
```

### Vercel Frontend Variables:

```bash
VITE_API_BASE_URL=https://your-backend.up.railway.app/api
```

⚠️ **NEVER** put backend API keys in `VITE_` variables - they're embedded in browser JavaScript!

---

## Step 13: Vapi Webhook URL 🔄 TODO

**Current Issue**: Settings endpoint returns wrong URL

### Fix Required:
Use `APP_PUBLIC_BASE_URL` environment variable to construct correct webhook URL:

```
https://${APP_PUBLIC_BASE_URL}/api/webhooks/vapi
```

Do NOT use `window.location.origin` - that's the Vercel frontend URL.

---

## Step 14: Security & Operations 🔄 TODO

### Add:
- [ ] Secure HTTP headers (helmet)
- [ ] Request size limits
- [ ] API timeouts (connection, read)
- [ ] Graceful shutdown (already added ✅)
- [ ] Error sanitization (no stack traces in prod)
- [ ] Log redaction (API keys, auth headers)
- [ ] Database backup strategy
- [ ] Railway metrics/logging docs
- [ ] Health vs readiness endpoints

### Do NOT Expose:
- API keys
- Database credentials
- Stack traces
- Environment values in endpoints

---

## Step 15: Testing & Verification 🔄 TODO

### Local Production-like Testing:

1. **Run with prod profile**:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export DB_URL=... # point to test DB
   # Set all required variables
   mvn spring-boot:run
   ```

2. **Build Docker image**:
   ```bash
   cd backend
   docker build -t agentops-crm:test .
   docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod ...
   ```

3. **Test frontend build**:
   ```bash
   cd frontend
   export VITE_API_BASE_URL=http://localhost:8080/api
   npm run build
   npm run preview
   ```

### Verify:
- [ ] Backend binds to supplied PORT
- [ ] Flyway migrates clean pgvector database
- [ ] `/actuator/health` returns 200
- [ ] CORS accepts configured origin, rejects others
- [ ] No hardcoded localhost in frontend
- [ ] Chat, crawl, RAG, leads, approvals work
- [ ] Vapi webhook returns Railway URL
- [ ] No secrets in built assets
- [ ] Startup fails clearly when required vars missing

---

## Deployment Steps

**DO NOT PROCEED** until all TODO items above are complete and all secrets are rotated.

### 1. Deploy Backend to Railway

```bash
# Commit all changes (except .env files)
git add backend/
git commit -m "chore: production configuration"
git push

# In Railway:
1. Create new project
2. Add PostgreSQL with pgvector
3. Add backend service
4. Connect to GitHub repo
5. Set root directory: /backend
6. Configure all environment variables
7. Deploy
8. Verify health check
```

### 2. Deploy Frontend to Vercel

```bash
# In Vercel:
1. Import GitHub repository
2. Set root directory: frontend
3. Set framework: Vite
4. Configure VITE_API_BASE_URL
5. Deploy
6. Test all routes
```

### 3. Post-Deployment

- [ ] Test production flows end-to-end
- [ ] Monitor Railway logs
- [ ] Check database migrations
- [ ] Verify pgvector index created
- [ ] Test CORS from production domain
- [ ] Confirm no localhost references

---

## Rollback Procedure

### If Backend Fails:
1. Check Railway logs
2. Verify environment variables
3. Check database connection
4. Roll back to previous Railway deployment
5. Check pgvector extension

### If Frontend Fails:
1. Check Vercel deployment logs
2. Verify API URL is correct
3. Check CORS configuration
4. Roll back to previous Vercel deployment

### If Database Migration Fails:
1. Check Flyway schema history
2. If V10 fails, verify pgvector extension
3. Manual rollback:
   ```sql
   DROP INDEX IF EXISTS idx_knowledge_chunks_embedding_vector;
   ALTER TABLE knowledge_chunks DROP COLUMN IF EXISTS embedding_vector;
   DROP EXTENSION IF EXISTS vector CASCADE;
   DELETE FROM flyway_schema_history WHERE version = '10';
   ```

---

## Support & Resources

### Documentation:
- Railway: https://docs.railway.app
- Vercel: https://vercel.com/docs
- pgvector: https://github.com/pgvector/pgvector
- Flyway: https://flywaydb.org/documentation

### Monitoring:
- Railway Metrics: Project → Metrics tab
- Vercel Analytics: Project → Analytics
- Railway Logs: `railway logs`

---

## Status Summary

| Step | Status | Blocker |
|------|--------|---------|
| 1. Secrets Rotation | ⚠️ **ACTION REQUIRED** | Must rotate Apify, OpenAI, Firecrawl |
| 2. Backend Config | ✅ Complete | None |
| 3. Railway PORT | ✅ Complete | None |
| 4. PostgreSQL Setup | 🔄 In Progress | Railway provisioning needed |
| 5. Flyway Migration | ✅ Complete | None |
| 6. Redis Config | 🔄 TODO | Determine if required |
| 7. CORS Config | 🔄 TODO | Implementation needed |
| 8. Frontend API | 🔄 TODO | Unify Axios instances |
| 9. Vercel Config | 🔄 TODO | Create vercel.json |
| 10. Dockerfile | 🔄 TODO | Create production Dockerfile |
| 11. Railway Config | 🔄 TODO | Create railway.toml |
| 12. Environment Vars | 🔄 TODO | Document all variables |
| 13. Vapi Webhook | 🔄 TODO | Fix URL construction |
| 14. Security | 🔄 TODO | Multiple enhancements |
| 15. Testing | 🔄 TODO | Full verification |

---

**⚠️ DEPLOYMENT READINESS: NOT READY**

**Next Actions**:
1. ✅ **IMMEDIATELY** rotate all exposed credentials
2. Complete remaining TODO items (steps 6-15)
3. Test thoroughly in production-like environment
4. Re-assess deployment readiness

---

*This guide will be updated as deployment preparation progresses.*
