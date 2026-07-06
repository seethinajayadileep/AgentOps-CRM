# Environment Configuration

## Backend Environment Variables

| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `SERVER_PORT` | No | 8080 | Server port | 8080 |
| `SPRING_PROFILES_ACTIVE` | No | dev | Active Spring profile | dev, prod |
| `DB_HOST` | Yes | localhost | PostgreSQL host | localhost |
| `DB_PORT` | Yes | 5432 | PostgreSQL port | 5432 |
| `DB_NAME` | Yes | agentops_crm | Database name | agentops_crm |
| `DB_USER` | Yes | postgres | Database username | postgres |
| `DB_PASSWORD` | Yes | postgres | Database password | secure_password |
| `REDIS_HOST` | Yes | localhost | Redis host | localhost |
| `REDIS_PORT` | Yes | 6379 | Redis port | 6379 |
| `REDIS_PASSWORD` | No | empty | Redis password (if required) | redis_password |
| `FIRECRAWL_API_KEY` | Yes | empty | Firecrawl API key for web crawling | fc-... |
| `OPENAI_API_KEY` | No | empty | OpenAI API key for embeddings | sk-... |
| `ANTHROPIC_API_KEY` | No | empty | Anthropic API key for AI | sk-ant-... |
| `VAPI_API_KEY` | No | empty | Vapi API key for voice calls | vapi-... |
| `ELEVENLABS_API_KEY` | No | empty | ElevenLabs API key for TTS | xi-... |
| `APIFY_ENABLED` | No | false | Enable the Apify Lead Finder (F-010). App starts fine when false/missing | true |
| `APIFY_API_TOKEN` | No | empty | Apify API token for lead discovery (server-side only, never sent to frontend) | apify_api_... |
| `APIFY_DEFAULT_ACTOR_ID` | No | empty | Default Apify actor id used when a run does not specify one | compass~crawler-google-places |
| `JWT_SECRET` | Yes | change-me | JWT signing secret | random_secret_key |
| `JWT_EXPIRATION` | No | 86400000 | JWT token expiration in ms | 86400000 |

### Backend .env.example
```bash
# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=agentops_crm
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# External Tools
FIRECRAWL_API_KEY=fc-...

# AI Services
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
VAPI_API_KEY=vapi-...
ELEVENLABS_API_KEY=xi-...

# Apify Lead Finder (F-010) - optional; app starts fine when disabled
APIFY_ENABLED=false
APIFY_API_TOKEN=apify_api_...
APIFY_DEFAULT_ACTOR_ID=compass~crawler-google-places

# JWT
JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION=86400000
```

## Frontend Environment Variables

| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `VITE_API_BASE_URL` | No | /api | Backend API base URL | http://localhost:8080/api |
| `VITE_APP_NAME` | No | AgentOps CRM | Application name | AgentOps CRM |

### Frontend .env.example
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=AgentOps CRM
```

## Infrastructure

### Docker Services

#### PostgreSQL
- **Image**: postgres:16-alpine
- **Port**: 5432
- **Default User**: postgres
- **Default Password**: postgres
- **Default Database**: agentops_crm

#### Redis
- **Image**: redis:7-alpine
- **Port**: 6379
- **Password**: None (can be configured)

### Starting Infrastructure
```bash
cd docker
docker-compose up -d
```

### Stopping Infrastructure
```bash
cd docker
docker-compose down
```

## Prerequisites

### Required Software
- **Java**: 21 or higher
- **Maven**: 3.9 or higher
- **Node.js**: 18 or higher
- **Docker**: 20.10 or higher
- **Docker Compose**: 2.0 or higher

### Verification Commands
```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check Node.js version
node -version

# Check Docker version
docker --version

# Check Docker Compose version
docker-compose --version
```

## Development vs Production

### Development
- Use `SPRING_PROFILES_ACTIVE=dev`
- Database: localhost Docker PostgreSQL
- Redis: localhost Docker Redis
- SQL logging: enabled
- Actuator: exposed

### Production
- Use `SPRING_PROFILES_ACTIVE=prod`
- Database: Managed PostgreSQL (AWS RDS, GCP Cloud SQL)
- Redis: Managed Redis (AWS ElastiCache)
- SQL logging: disabled
- Actuator: restricted
- All secrets from environment variables

## Security Notes

1. **Never commit .env files** to version control
2. **Use strong secrets** for JWT_SECRET in production
3. **Use managed services** for database and Redis in production
4. **Enable HTTPS** in production
5. **Rotate API keys** regularly
6. **Use different secrets** for development and production

## Secret Management (Production)

Recommended approaches:
- AWS Secrets Manager
- HashiCorp Vault
- Environment variables from CI/CD
- Kubernetes Secrets

## Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs agentops-postgres

# Test connection
docker exec agentops-postgres psql -U postgres -c "SELECT 1"
```

### Redis Connection Issues
```bash
# Check Redis is running
docker ps | grep redis

# Test connection
docker exec agentops-redis redis-cli ping
```

### Port Conflicts
If port 8080 is in use, change it in application.properties:
```properties
server.port=8081
```