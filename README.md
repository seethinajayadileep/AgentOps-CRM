# AgentOps CRM - Spring Boot Multi-Agent AI Platform

![Version](https://img.shields.io/badge/version-0.1.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![React](https://img.shields.io/badge/React-18.2.0-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5.2.2-blue)

A production-style full-stack agentic AI CRM platform that automates customer interactions, lead qualification, and follow-ups using AI agents.

## 🎯 Project Goals

1. **Web-Based Knowledge Base**: Automatically crawl business websites to build AI knowledge
2. **RAG-Powered Chat**: Answer customer questions using only verified business knowledge
3. **Intelligent Lead Qualification**: Automatically qualify leads from chat conversations
4. **AI Voice Automation**: Make approved voice calls using Vapi
5. **Daily Voice Reports**: Generate executive summaries with ElevenLabs
6. **Lead Discovery**: Find potential leads using Apify
7. **Complete Audit Trail**: Track every agent action for transparency

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend (React)                       │
│  Dashboard | Businesses | Leads | Conversations | Voice     │
└─────────────────────────┬───────────────────────────────────┘
                          │ REST API
┌─────────────────────────▼───────────────────────────────────┐
│                    Backend (Spring Boot)                     │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Controller  │  │   Service    │  │  Repository  │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │              │
│  ┌──────▼─────────────────▼─────────────────▼───────┐     │
│  │              Business Logic Layer                 │     │
│  │  Agent Orchestrator | RAG Engine | Lead Qualifier│     │
│  └──────────────────────────────────────────────────┘     │
└─────────────────────────┬───────────────────────────────────┘
                          │
    ┌─────────────────────┼─────────────────────┐
    │                     │                     │
┌───▼────┐          ┌─────▼─────┐         ┌─────▼─────┐
│PostgreSQL│         │  Redis    │         │   AI APIs  │
└────────┘          └───────────┘         └───────────┘
                                         Firecrawl|Vapi|
                                         ElevenLabs|Apify
```

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 3.2.5 | Application framework |
| Spring Data JPA | 3.2.5 | Database ORM |
| PostgreSQL | 16 | Primary database |
| Redis | 7 | Cache & queue |
| Flyway | 10.0+ | Database migrations |
| Maven | 3.9+ | Build tool |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2.0 | UI framework |
| Vite | 5.2.0 | Build tool |
| TypeScript | 5.2.2 | Type safety |
| Tailwind CSS | 3.4.3 | Styling |
| Axios | 1.6.8 | HTTP client |
| React Router | 6.22.3 | Navigation |

### External AI Tools
| Tool | Purpose |
|------|---------|
| Firecrawl | Website crawling |
| Vapi | AI voice calls |
| ElevenLabs | Text-to-speech |
| Apify | Lead discovery |

## 📦 Project Structure

```
agentops-crm/
├── backend/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agentopscrm/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data transfer objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── repository/      # JPA repositories
│   │   │   │   ├── service/         # Business logic
│   │   │   │   ├── exception/       # Custom exceptions
│   │   │   │   ├── client/          # External API clients
│   │   │   │   ├── agent/           # Agent orchestration
│   │   │   │   ├── rag/             # RAG implementation
│   │   │   │   ├── security/        # Security & auth
│   │   │   │   └── util/            # Utilities
│   │   │   └── resources/
│   │   │       ├── prompts/         # AI prompt templates
│   │   │       └── migration/       # Flyway migrations
│   │   └── test/
│   └── pom.xml
├── frontend/                # React + Vite frontend
│   ├── src/
│   │   ├── api/             # API client functions
│   │   ├── components/      # React components
│   │   ├── pages/           # Page components
│   │   ├── layouts/         # Layout components
│   │   ├── hooks/           # Custom hooks
│   │   ├── types/           # TypeScript types
│   │   └── utils/           # Utility functions
│   ├── package.json
│   └── vite.config.ts
├── docs/                    # Documentation
│   ├── PROJECT_SPEC.md
│   ├── FEATURE_CHECKLIST.md
│   ├── API_CONTRACT.md
│   ├── CHANGELOG.md
│   ├── DECISIONS.md
│   ├── FILE_MAP.md
│   ├── DEBUG_LOG.md
│   ├── TEST_PLAN.md
│   ├── ENVIRONMENT.md
│   └── ROADMAP.md
├── docker/                  # Docker configurations
│   └── docker-compose.yml
├── README.md
└── .gitignore
```

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.9** or higher
- **Node.js 22** (Vercel / frontend)
- **Docker & Docker Compose** (for PostgreSQL and Redis)
- **PostgreSQL 16** or higher
- **Redis 7** or higher

### 1. Clone the Repository

```bash
git clone <repository-url>
cd agentops-crm
```

### 2. Start Infrastructure

```bash
cd docker
docker-compose up -d
```

This will start:
- PostgreSQL on port 5433 (5432 conflicts with local PostgreSQL on macOS)
- Redis on port 6379

### 3. Configure Environment Variables

Create `.env` files:

**Backend (.env):**
```bash
# Database
DB_HOST=localhost
DB_PORT=5433
DB_NAME=agentops_crm
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# External Tools (Required for crawling)
FIRECRAWL_API_KEY=fc-...

# API Keys (add as needed)
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
VAPI_API_KEY=vapi-...
ELEVENLABS_API_KEY=xi-...
APIFY_API_TOKEN=at-...
```

**Frontend (.env):**
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=AgentOps CRM
```

### 4. Run Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend will be available at `http://localhost:8080`

### 5. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at `http://localhost:5173`

### 6. Verify Setup

Check backend health:
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2026-07-01T12:00:00Z",
  "services": {
    "database": "UP",
    "redis": "UP"
  },
  "version": "0.1.0"
}
```

## ☁️ Deploy (Vercel frontend + Railway backend)

GitHub auto-deploy is already wired to [AgentOps-CRM](https://github.com/seethinajayadileep/AgentOps-CRM). This is a monorepo:

| Host | Root directory | Config |
|------|----------------|--------|
| **Vercel** | `frontend` (or repo root + `vercel.json`) | Vite build → `dist` |
| **Railway** | `backend` (or repo root + `Dockerfile`) | Java 21 Docker image |

### Vercel

Set at **build** time (Vite inlines it):

- `VITE_API_BASE_URL` — Railway public URL **including** `/api`  
  Example: `https://your-service.up.railway.app/api`

### Railway

- `SPRING_PROFILES_ACTIVE=prod`
- `PORT` — injected by Railway
- Database: set `DB_URL` (`jdbc:postgresql://...`) or link Railway Postgres (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`)
- `CORS_ALLOWED_ORIGINS` — production Vercel origin (preview URLs `https://*.vercel.app` are allowed automatically)
- `PUBLIC_BACKEND_URL` — Railway public HTTPS origin
- Provider keys: `OPENAI_API_KEY`, `FIRECRAWL_API_KEY`, `VAPI_*`, `APIFY_*` as used locally

Postgres needs the **pgvector** extension. Do not commit `.env` files; use `frontend/.env.example` and `backend/.env.example`.

## 🧪 Testing

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test

# E2E tests
npm run test:e2e
```



## 👥 Team

- **Backend**: Java Spring Boot
- **Frontend**: React + Vite + TypeScript
