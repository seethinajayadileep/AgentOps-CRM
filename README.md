# AgentOps CRM

A CRM where the agents are part of the product, not a sidebar chatbot.

You add a company, crawl its site, and the app answers support questions from that knowledge. If someone sounds interested, it captures a lead. You can also hunt prospects with Apify and call them with Vapi. Follow-up copy sits in Approvals until a person says yes.

**Live:** https://agent-ops-crm.vercel.app/

Demo login (seeded on first boot):

```
demo@agentcrm.app
Demo@123
```

Repo: https://github.com/seethinajayadileep/AgentOps-CRM

## Why this exists

Most CRMs store contacts. This one is meant to *do* the next step: research the business, talk to the visitor, qualify them, and (if you turn the keys on) find more leads and place a voice call.

The risky bits still go through a person. The AI drafts a follow-up; you approve or reject it. Voice calls are the exception: once Vapi is configured, Start Voice Call actually dials.

## What a typical run looks like

1. Sign in, add a business with a real website.
2. Crawl it (Firecrawl). Build the knowledge base (OpenAI embeddings in Postgres/pgvector).
3. Ask the support chat something only that site would know. RAG answers from the chunks, not from thin air.
4. If the visitor asks for a quote and leaves a name plus email or phone, a lead shows up on the Leads page.
5. Optional: Lead Finder (Apify) for outbound prospects, import the ones you want.
6. Optional: start a Vapi call from a lead that has a phone number. The transcript comes back on Voice Calls.
7. Approvals and Agent Logs are the paper trail.

Settings tells you which integrations are actually wired. Redis is optional: login throttling and a short dashboard cache. The API still boots without it.

## Stack

**API** — Java 21, Spring Boot 3.2, Spring Security (JWT), JPA, Flyway. Runs on Railway.

**App** — React 18, Vite, TypeScript, Tailwind. Runs on Vercel.

**Data** — PostgreSQL 16 with pgvector. Do not use stock Postgres in production; Flyway needs the `vector` extension.

Auth is email/password. The SPA keeps a JWT and also sets an httpOnly cookie (`agentcrm_session`).

## Layout

```
backend/     Spring Boot API
frontend/    Vite SPA
docker/      local Postgres (and optional Redis)
```

The interesting Java packages: `agent/` (qualification, evaluation), `client/` (Firecrawl, Apify, Vapi), `service/`, `controller/`. Frontend pages live under `frontend/src/pages/`.

## Run it locally

You need Java 21, Maven, Node 24 (see `.nvmrc`), and Docker for Postgres.

```bash
# database — published on 5433 so it does not fight a local Postgres on 5432
docker compose -f docker/docker-compose.yml up -d postgres

cp backend/.env.example backend/.env
# put OPENAI_API_KEY in backend/.env if you want chat / RAG to work

cd backend
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://127.0.0.1:5433/agentops_crm
export DB_USER=postgres
export DB_PASSWORD=postgres
mvn spring-boot:run
```

API: http://localhost:8080 — `curl http://localhost:8080/api/health`

```bash
cd frontend
npm ci
npm run dev
```

UI: http://localhost:5173. Vite proxies `/api` to the local backend. Do not point local Vite at Railway.

```bash
cd backend && mvn test
cd frontend && npm test
```

Playwright (`npm run test:e2e` in `frontend`) wants both the API and Vite running.

## Hosting

| Piece | Where | Root |
|-------|--------|------|
| SPA | Vercel | `frontend` |
| API | Railway | `backend` |
| Postgres | Railway, **pgvector** template | — |

Push to GitHub, then attach both hosts to this repo. Set the root directories above so each host finds its own Dockerfile / `vercel.json`.

**Railway API** needs at least:

- `JWT_SECRET` — long random string, required in prod (no default)
- Database link so `DATABASE_URL` or `DB_URL` / `DB_USER` / `DB_PASSWORD` exist. `DB_URL` must be `jdbc:postgresql://...`
- `CORS_ALLOWED_ORIGINS` — your Vercel origin, e.g. `https://your-app.vercel.app`
- `PUBLIC_BACKEND_URL` — public Railway HTTPS URL, no trailing slash
- `OPENAI_API_KEY` — without this, RAG and lead capture will not do much

Healthcheck path is `/api/health` (`backend/railway.toml`). `PORT` comes from Railway.

Optional: `FIRECRAWL_API_KEY`, `APIFY_ENABLED` + `APIFY_API_TOKEN`, `VAPI_ENABLED` + assistant/phone/webhook vars, `REDIS_URL` (rate limits + dashboard cache), `AUTH_SEED_DEMO_USER`.

If a replica never becomes healthy, open **Deploy Logs** and look for a missing `JWT_SECRET`, a missing database, or Flyway failing on `CREATE EXTENSION vector` (wrong Postgres image).

**Vercel** needs one build-time variable:

```
VITE_API_BASE_URL=https://your-service.up.railway.app/api
```

Include `/api`. Change it, then redeploy the frontend — Vite bakes this in at build time.

After both are up: put the exact Vercel origin in `CORS_ALLOWED_ORIGINS`, confirm login, and point Vapi webhooks at `https://<railway>/api/webhooks/vapi` if you use voice.

Cookies in prod are `Secure` + `SameSite=None` so the Vercel origin can hold the session. Bearer tokens still work if the browser blocks third-party cookies.

Do not commit `.env`. Templates are `backend/.env.example` and `frontend/.env.example`.

## Security, short version

Public: `/api/auth/**`, `/api/health`, `/actuator/**`, `/api/webhooks/**`. Everything else under `/api` needs a JWT. CORS is origin-based with credentials, never `*` plus cookies.
