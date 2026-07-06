# AgentOps CRM - Spring Boot Multi-Agent Voice AI Platform

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
- **Node.js 18** or higher
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

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [PROJECT_SPEC.md](docs/PROJECT_SPEC.md) | Complete project specification |
| [FEATURE_CHECKLIST.md](docs/FEATURE_CHECKLIST.md) | Feature tracking with IDs |
| [API_CONTRACT.md](docs/API_CONTRACT.md) | API endpoint documentation |
| [CHANGELOG.md](docs/CHANGELOG.md) | Change history |
| [DECISIONS.md](docs/DECISIONS.md) | Technical decisions |
| [FILE_MAP.md](docs/FILE_MAP.md) | File purpose mapping |
| [DEBUG_LOG.md](docs/DEBUG_LOG.md) | Debug issues log |
| [TEST_PLAN.md](docs/TEST_PLAN.md) | Test strategies |
| [ENVIRONMENT.md](docs/ENVIRONMENT.md) | Environment setup |
| [ROADMAP.md](docs/ROADMAP.md) | Development roadmap |

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

## 📝 Development Guidelines

1. **Build step by step** - Do not implement full features at once
2. **Update docs before coding** - Update FEATURE_CHECKLIST.md with planned tasks
3. **Update docs after coding** - Update CHANGELOG.md with exact changes
4. **Track all files** - Add new files to FILE_MAP.md with purpose
5. **Document all APIs** - Add endpoints to API_CONTRACT.md
6. **Log debug issues** - Write problems to DEBUG_LOG.md with root cause
7. **Document decisions** - Add technical decisions to DECISIONS.md
8. **Store prompts** - Save AI prompts in backend/src/main/resources/prompts/
9. **Never hide changes** - Always explain what was changed and why
10. **No fake logic** - Mark placeholder code clearly as TODO
11. **Protect secrets** - Use environment variables, never expose keys in frontend

## 🗺️ Roadmap

See [ROADMAP.md](docs/ROADMAP.md) for complete development timeline.

### Current Phase: Phase 0 - Project Foundation ✅

**Completed:**
- ✅ Monorepo structure
- ✅ Backend skeleton
- ✅ Frontend skeleton
- ✅ Documentation files
- ✅ Health endpoint

**Next: Phase 1 - Core Infrastructure**
- Database schema
- Redis integration
- Security layer
- Enhanced health checks

## 📄 License

This project is a resume-level educational project.

## 👥 Team

- **Backend**: Java Spring Boot
- **Frontend**: React + Vite + TypeScript
- **AI Integration**: Spring AI / LangChain4j

---

**Version**: 0.1.0
**Last Updated**: 2026-07-01# AgentOps-CRM
