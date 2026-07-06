# AgentOps CRM — Spring Boot Multi-Agent Voice AI Platform

## Project Overview

AgentOps CRM is a production-style full-stack agentic AI CRM platform that helps businesses automate customer interactions, lead qualification, and follow-ups using AI agents.

### Core Value Proposition

- **Web-Based Knowledge Base**: Automatically crawl business websites to build AI knowledge
- **RAG-Powered Chat**: Answer customer questions using only verified business knowledge
- **Intelligent Lead Qualification**: Automatically qualify leads from chat conversations
- **AI Voice Automation**: Make approved voice calls using Vapi
- **Daily Voice Reports**: Generate executive summaries with ElevenLabs
- **Lead Discovery**: Find potential leads using Apify
- **Complete Audit Trail**: Track every agent action for transparency

## Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| Java 21 | Language |
| Spring Boot 3.x | Application framework |
| Spring Web | REST API |
| Spring Data JPA | Database ORM |
| PostgreSQL | Primary database |
| Redis | Cache & queue |
| Flyway | Database migrations |
| Spring AI / LangChain4j | AI integration |
| Actuator | Health monitoring |

### Frontend
| Technology | Purpose |
|------------|---------|
| React | UI framework |
| Vite | Build tool |
| TypeScript | Type safety |
| Tailwind CSS | Styling |
| Axios | HTTP client |
| React Router | Navigation |

### External AI Tools
| Tool | Purpose |
|------|---------|
| Firecrawl | Website crawling |
| Vapi | AI voice calls |
| ElevenLabs | Text-to-speech |
| Apify | Lead discovery |

## Architecture

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

## Key Features

| Feature ID | Feature | Status |
|------------|---------|--------|
| F-001 | Business Onboarding | NOT_STARTED |
| F-002 | Website Crawling | NOT_STARTED |
| F-003 | RAG Knowledge Base | NOT_STARTED |
| F-004 | Chat Widget | NOT_STARTED |
| F-005 | Lead Qualification | NOT_STARTED |
| F-006 | Follow-up Generation | NOT_STARTED |
| F-007 | AI Voice Calls | NOT_STARTED |
| F-008 | Voice Reports | NOT_STARTED |
| F-009 | Lead Discovery | NOT_STARTED |
| F-010 | Agent Logging | NOT_STARTED |

## Environment Variables

See [ENVIRONMENT.md](ENVIRONMENT.md) for complete configuration.

## Getting Started

See [ROADMAP.md](ROADMAP.md) for development phases.

## Running the Project

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## API Documentation

See [API_CONTRACT.md](API_CONTRACT.md) for all endpoints.

## Testing

See [TEST_PLAN.md](TEST_PLAN.md) for test strategies.

---

**Version**: 1.0.0
**Last Updated**: 2026-07-01