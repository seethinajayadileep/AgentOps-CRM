# AgentOps CRM

![Version](https://img.shields.io/badge/version-0.2.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![React](https://img.shields.io/badge/React-18.2.0-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5.2.2-blue)

Agentic revenue operations CRM: discover leads, crawl business knowledge, run grounded conversations, and keep humans in the loop for follow-ups and voice calls.

**Repo:** [seethinajayadileep/AgentOps-CRM](https://github.com/seethinajayadileep/AgentOps-CRM)

Production hosting for this monorepo:

| Piece | Host | Root directory |
|-------|------|----------------|
| React + Vite SPA | [Vercel](https://vercel.com) | `frontend` |
| Spring Boot API | [Railway](https://railway.app) | `backend` |
| PostgreSQL + pgvector | Railway | pgvector template (not stock Postgres) |

## What it does

- **Marketing site** at `/`, plus login and signup
- **Workspace** (after sign-in): dashboard, businesses, leads, lead finder, conversations, voice calls, approvals, agent logs, settings
- **Knowledge base:** Firecrawl site crawl → chunk → OpenAI embeddings → pgvector retrieval
- **Approvals:** drafted follow-ups and outbound actions wait for a human decision
- **Integrations:** OpenAI, Firecrawl, Apify, Vapi (each optional until you enable it)

Redis is **not used**. You do not need a Redis service on Railway.

## Tech stack

| Layer | Stack |
|-------|--------|
| API | Java 21, Spring Boot 3.2.5, Spring Security (JWT), Spring Data JPA, Flyway |
| App | React 18, Vite 5, TypeScript 5.2, Tailwind CSS 3.4, Axios, React Router 6 |
| Data | PostgreSQL 16 with [pgvector](https://github.com/pgvector/pgvector) |
| Auth | Email/password, httpOnly session cookie `agentcrm_session`, `Authorization: Bearer` |

## Repository layout

```
AgentOps-CRM/
├── backend/                  Spring Boot API (Railway)
│   ├── Dockerfile
│   ├── railway.toml
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/agentopscrm/
│       │   ├── agent/        Qualification and evaluation agents
│       │   ├── client/       Firecrawl, Vapi, Apify
│       │   ├── config/       CORS, security
│       │   ├── controller/   REST
│       │   ├── security/     JWT filter
│       │   └── service/
│       └── resources/
│           ├── application.yml
│           ├── application-prod.yml
│           └── migration/    Flyway (includes pgvector)
├── frontend/                 Vite SPA (Vercel)
│   ├── vercel.json
│   └── src/
│       ├── api/              Axios client (withCredentials)
│       ├── auth/             Session context
│       ├── components/
│       ├── pages/            Marketing, auth, and workspace
│       └── types/
├── docker/docker-compose.yml Local Postgres (port 5433) + optional Redis
├── vercel.json               Fallback if Vercel root is the repo
├── Dockerfile                Fallback if Railway root is the repo
└── README.md
```

## Local development

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22 (see `.nvmrc`)
- Docker Desktop (recommended for Postgres)

### 1. Database

```bash
docker compose -f docker/docker-compose.yml up -d postgres
```

This starts **pgvector/pgvector:0.7.4-pg16** as `agentops-postgres` on **host port 5433** (5432 is often taken by a local Postgres).

You can skip the Compose Redis service. The API does not connect to Redis.

### 2. Backend

```bash
cp backend/.env.example backend/.env
# Set OPENAI_API_KEY (and other keys you want to exercise).
# JWT_SECRET can stay as the local placeholder on the dev profile only.

cd backend
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://127.0.0.1:5433/agentops_crm
export DB_USER=postgres
export DB_PASSWORD=postgres
mvn spring-boot:run
```

API: `http://localhost:8080`

```bash
curl http://localhost:8080/api/health
```

Example:

```json
{
  "status": "UP",
  "timestamp": "2026-08-31T10:30:00Z",
  "services": {
    "application": { "status": "UP", "message": "AgentOps CRM API" },
    "redis": { "status": "DISABLED", "message": "Redis is not required by this application" }
  },
  "version": "0.2.0"
}
```

Deploy probes should use `GET /actuator/health`.

### 3. Frontend

```bash
cd frontend
npm ci
npm run dev
```

Vite: `http://localhost:5173`. In development, `/api` is proxied to `http://localhost:8080`. Do not point local Vite at a remote Railway URL.

### Sample account

On first boot the API seeds:

| Field | Value |
|-------|--------|
| Email | `demo@agentcrm.app` |
| Password | `Demo@123` |

Disable later with `AUTH_SEED_DEMO_USER=false`. Sign up at `/signup` for a real account.

### Tests

```bash
cd backend && mvn test
cd frontend && npm test
cd frontend && npm run test:e2e   # needs API + Vite running
```

---

## Deploy: Railway backend + Vercel frontend

Push to GitHub, then wire each host to this repository. Set **Root Directory** as in the table above so the nested `Dockerfile` / `vercel.json` are used.

### A. Railway — Postgres with pgvector

Stock Railway Postgres **does not** include pgvector. Flyway `V10__add_pgvector_support.sql` runs `CREATE EXTENSION vector` and will fail without it.

1. In the same Railway project, deploy a **Postgres + pgvector** template ([Railway pgvector template](https://railway.com/deploy/postgres-with-pgvector-engine)).
2. In a query console, run once if the template did not already:

   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

3. Use a **fresh empty database**. Do not set Flyway `baseline-on-migrate` in production.

### B. Railway — API service

1. **New service → GitHub** → this repo.
2. **Root Directory:** `backend`
3. Builder: Dockerfile (`backend/Dockerfile`). `SPRING_PROFILES_ACTIVE=prod` is set in the image and in `backend/railway.toml`.
4. **Settings → Networking:** generate a public HTTPS domain (e.g. `https://agentops-crm-production.up.railway.app`).
5. **Healthcheck path:** `/actuator/health` (already in `railway.toml`).
6. Variables (Variables tab). Reference the pgvector service instead of pasting passwords:

| Variable | Value |
|----------|--------|
| `JWT_SECRET` | `openssl rand -base64 64` — required, no default in prod |
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `CORS_ALLOWED_ORIGINS` | Production frontend origin, e.g. `https://your-app.vercel.app` (preview hosts `https://*.vercel.app` are already allowed) |
| `PUBLIC_BACKEND_URL` | Public Railway HTTPS origin, **no trailing slash** |
| `OPENAI_API_KEY` | Required for embeddings and RAG answers |
| `FIRECRAWL_API_KEY` | Optional until you crawl sites |
| `APIFY_ENABLED` / `APIFY_API_TOKEN` | Optional until Lead Finder |
| `VAPI_ENABLED` / `VAPI_API_KEY` / `VAPI_ASSISTANT_ID` / `VAPI_PHONE_NUMBER_ID` / `VAPI_WEBHOOK_SECRET` | Optional until voice |
| `AUTH_SEED_DEMO_USER` | `true` to keep the demo login; `false` otherwise |

`PORT` is injected by Railway. If the database service is not named `Postgres`, use Railway’s variable-reference picker so the `${{ServiceName.PGHOST}}` names match.

Cookies in prod are `Secure` + `SameSite=None` so the Vercel origin can receive them. The SPA also stores the JWT and sends `Authorization: Bearer`, which still works if the browser blocks third-party cookies.

Do not commit `.env`. After the first successful migrate, `GET https://<railway>/api/health` should return `"version": "0.2.0"`.

### C. Vercel — frontend

1. [Import](https://vercel.com/new) the same GitHub repo.
2. **Root Directory:** `frontend` (Framework Preset: Vite, Node 22).
3. Environment variable (**Production**, and Preview if you use preview deploys):

| Variable | Value |
|----------|--------|
| `VITE_API_BASE_URL` | Railway public URL **including** `/api`, e.g. `https://your-service.up.railway.app/api` |

Vite inlines `VITE_*` at **build** time. Change the URL → redeploy the frontend.

4. Deploy. Open the Vercel URL, sign in, and confirm the dashboard loads live data.

If you instead leave Vercel root at the repository, the root `vercel.json` builds `frontend` and publishes `frontend/dist`. Prefer **Root Directory = `frontend`**.

### D. After both are live

1. Put the exact Vercel production origin in Railway `CORS_ALLOWED_ORIGINS` (comma-separated if you also have a custom domain).
2. Confirm login from the Vercel origin succeeds (`POST /api/auth/login` is 200, then `/dashboard` shows counts).
3. Point Vapi webhooks at `https://<railway>/api/webhooks/vapi` when you enable voice.
4. Rotate any key that ever lived in git. `JWT_SECRET` must not be the local placeholder; prod refuses to boot if it is.

---

## Environment reference

Templates: `backend/.env.example`, `frontend/.env.example`.

| Variable | Where | Notes |
|----------|--------|--------|
| `SPRING_PROFILES_ACTIVE` | Railway | `prod` |
| `JWT_SECRET` | Railway | Required in prod; min 32 characters |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Railway | `jdbc:postgresql://...` (not `postgresql://`) |
| `CORS_ALLOWED_ORIGINS` | Railway | Vercel production (and custom) origins |
| `PUBLIC_BACKEND_URL` | Railway | Public API origin for webhook URLs |
| `AUTH_COOKIE_SECURE` / `AUTH_COOKIE_SAMESITE` | Local | Prod profile forces `true` / `None` |
| `SHOWCASE_EXTERNAL_ACTIONS_DISABLED` | Either | Default `false` — operators can approve, search, call, delete |
| `VITE_API_BASE_URL` | Vercel | Must include `/api` |

## Auth and security notes

- Public: `/api/auth/**`, `/api/health`, `/actuator/**`, `/api/webhooks/**`
- Everything else under `/api/**` requires a valid JWT
- CORS uses origin **patterns** with credentials (never `*` + cookies)
- Session cookie: `agentcrm_session`, path `/`, httpOnly
- Do not log or commit secrets

## License

Private / unlicensed unless otherwise stated in the repository settings.
