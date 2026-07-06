# 🚨 CRITICAL SECURITY AUDIT - EXPOSED CREDENTIALS

**DATE**: 2026-07-06  
**STATUS**: ⚠️ CRITICAL - IMMEDIATE ACTION REQUIRED

## Exposed Credentials Found

### 1. Apify API Token (EXPOSED - MUST ROTATE)
**Token**: `apify_api_8dc8...iphr0cmSIC` (redacted)

**Locations**:
- `.env` (line 25) 
- `backend/.env` (line 39)
- `backend/.env.example` (line 44) ❌ **SHOULD BE PLACEHOLDER**
- `backend.log` (multiple URLs in debug logs)
- `run-restart.log` (in logged API calls)

**Impact**: CRITICAL - Token can access/execute Apify actors, potentially incurring costs

**Action Required**:
1. ✅ **IMMEDIATELY** revoke this token at https://console.apify.com/account/integrations
2. Generate new token
3. Update `.env` and `backend/.env` with new token
4. Remove from `.env.example` (replace with placeholder)
5. Add to `.gitignore` if not already present

---

### 2. OpenAI API Key (EXPOSED - MUST ROTATE)
**Key**: `sk-proj-e19...plFohhfUA` (redacted)

**Locations**:
- `.env` (line 5)
- `backend/.env` (line 20)

**Impact**: CRITICAL - Unlimited API usage, potential for abuse, financial damage

**Action Required**:
1. ✅ **IMMEDIATELY** revoke at https://platform.openai.com/api-keys
2. Generate new key
3. Update `.env` and `backend/.env`
4. Monitor OpenAI usage dashboard for unauthorized usage

---

### 3. Firecrawl API Key (EXPOSED - MUST ROTATE)
**Key**: `fc-663f92664f5d4cc69c38e88550d3b728`

**Locations**:
- `.env` (line 8)
- `backend/.env` (line 23)

**Impact**: HIGH - Can scrape websites using your Firecrawl account

**Action Required**:
1. ✅ Revoke at https://firecrawl.dev (account settings)
2. Generate new key
3. Update `.env` and `backend/.env`

---

### 4. Database Passwords (LOW RISK - Development Only)
**Password**: `postgres` (default)

**Locations**:
- `.env`
- `backend/.env`
- `backend/.env.example` (line 13)

**Impact**: LOW - Only localhost development database
**Action**: No immediate rotation needed for dev, but ensure production uses strong passwords

---

### 5. JWT Secret (WEAK - MUST CHANGE)
**Secret**: `your-secret-key-change-in-production`

**Location**:
- `backend/.env.example` (line 50)

**Impact**: MEDIUM - Weak placeholder secret
**Action**: Generate strong secret for production (min 256-bit random)

---

## Files That Must Be Updated

### Immediate (Before Any Commit)

1. **`backend/.env.example`** (line 44)
   - Change: `APIFY_API_TOKEN=apify_api_8dc8...iphr0cmSIC` (exposed token - redacted)
   - To: `APIFY_API_TOKEN=apify_api_YOUR_TOKEN_HERE`

### After Rotating Keys (Do Not Commit)

2. **`.env`** (root) - Update all rotated keys
3. **`backend/.env`** - Update all rotated keys

### Logged Secrets (Already Committed - Cannot Fix)

4. **`backend.log`** - Contains Apify token in RestTemplate URLs
5. **`run-restart.log`** - Contains Apify token in logged URLs
6. **`PRODUCTION_SECURITY_AUDIT_AND_PLAN.md`** - Documents the exposed keys

⚠️ **Note**: Log files with exposed credentials are already tracked in git history and cannot be fully removed without rewriting history (not recommended for shared repos).

---

## Git Files to Check

Run this command to verify secrets aren't tracked:
```bash
git ls-files | grep -E '\.env$|\.log$'
```

Expected `.gitignore` entries:
```
.env
.env.local
.env.*.local
*.log
backend/.env
backend/*.log
```

---

## Credential Rotation Checklist

### Before Deployment

- [ ] **Apify Token** - Rotated and updated
- [ ] **OpenAI Key** - Rotated and updated
- [ ] **Firecrawl Key** - Rotated and updated
- [ ] **Backend .env.example** - Sanitized with placeholders only
- [ ] **JWT Secret** - Strong random secret generated for production
- [ ] **Vapi Keys** rot (if any were exposed - to verify)
- [ ] **Admin Password** - Hashed, not plain text

### Production Environment Variables (Railway)

Will create strong production credentials:
- `APIFY_API_TOKEN` - New token from Apify dashboard
- `OPENAI_API_KEY` - New key from OpenAI
- `FIRECRAWL_API_KEY` - New key from Firecrawl
- `JWT_SECRET` - Cryptographically random (e.g., `openssl rand -base64 64`)
- `ADMIN_PASSWORD_HASH` - Bcrypt hashed password
- `VAPI_WEBHOOK_SECRET` - Random secret for webhook validation
- `DB_PASSWORD` - Strong Railway-generated password

---

## Immediate Next Steps

1. ✅ **STOP** all development
2. ✅ **ROTATE** Apify, OpenAI, and Firecrawl credentials immediately
3. ✅ Update `.env` files with new credentials (DO NOT COMMIT)
4. ✅ Sanitize `backend/.env.example` with placeholders
5. ✅ Verify `.gitignore` contains `.env` and `*.log`
6. ✅ Commit sanitized `.env.example` only
7. ✅ Continue with production deployment preparation

---

## Prevention for Future

1. **Pre-commit hooks**: Add `detect-secrets` or `git-secrets`
2. **Code review**: Ensure no credentials in code
3. **Environment variable validation**: Fail startup if example credentials detected
4. **Separate .env files**: Never commit `.env`, only `.env.example` with placeholders

---

## Contact for Key Rotation

- **Apify**: https://console.apify.com/account/integrations
- **OpenAI**: https://platform.openai.com/api-keys  
- **Firecrawl**: https://firecrawl.dev/dashboard (Settings → API Keys)

---

**CRITICAL**: Do NOT deploy to production until ALL exposed credentials are rotated.
