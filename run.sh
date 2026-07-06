#!/usr/bin/env bash
#
# AgentOps CRM - start the whole stack (Postgres + Redis + backend + frontend) at once.
#
# Usage:
#   export OPENAI_API_KEY=sk-...          # required (embeddings + RAG answers)
#   export FIRECRAWL_API_KEY=fc-...       # optional (only needed to crawl new sites)
#   ./run.sh
#
# Or put those two lines in a .env file at the repo root (it is git-ignored) and just run ./run.sh
#
# Press Ctrl+C to stop the backend + frontend. Docker keeps running (use ./stop.sh to stop everything).

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# Load keys from an optional, git-ignored .env at the repo root.
if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

: "${OPENAI_API_KEY:?OPENAI_API_KEY is required. Run: export OPENAI_API_KEY=sk-... (or add it to ./.env)}"
FIRECRAWL_API_KEY="${FIRECRAWL_API_KEY:-}"

# Vapi configuration (F-008)
VAPI_ENABLED="${VAPI_ENABLED:-false}"
VAPI_API_KEY="${VAPI_API_KEY:-}"
VAPI_ASSISTANT_ID="${VAPI_ASSISTANT_ID:-}"
VAPI_PHONE_NUMBER_ID="${VAPI_PHONE_NUMBER_ID:-}"
VAPI_WEBHOOK_SECRET="${VAPI_WEBHOOK_SECRET:-}"

# Apify Lead Finder configuration (F-010) - optional
APIFY_ENABLED="${APIFY_ENABLED:-false}"
APIFY_API_TOKEN="${APIFY_API_TOKEN:-}"
APIFY_DEFAULT_ACTOR_ID="${APIFY_DEFAULT_ACTOR_ID:-}"

# Java 25 host? The app targets Java 21; this flag lets Hibernate/ByteBuddy run on newer JDKs.
JVM_ARGS="-Xmx2g -Dnet.bytebuddy.experimental=true"

echo "==> Freeing ports 8080 (backend) and 5173 (frontend) if in use"
lsof -ti:8080 2>/dev/null | xargs -r kill -9 2>/dev/null || true
lsof -ti:5173 2>/dev/null | xargs -r kill -9 2>/dev/null || true

echo "==> Starting Postgres + Redis (Docker)"
docker compose -f docker/docker-compose.yml up -d

echo "==> Waiting for Postgres to be healthy"
for _ in $(seq 1 30); do
  s="$(docker inspect -f '{{.State.Health.Status}}' agentops-postgres 2>/dev/null || echo starting)"
  [ "$s" = "healthy" ] && { echo "    postgres healthy"; break; }
  sleep 2
done

echo "==> Starting backend  ->  http://localhost:8080   (logs: backend.log)"
(
  cd backend
  OPENAI_API_KEY="$OPENAI_API_KEY" FIRECRAWL_API_KEY="$FIRECRAWL_API_KEY" \
  VAPI_ENABLED="$VAPI_ENABLED" VAPI_API_KEY="$VAPI_API_KEY" \
  VAPI_ASSISTANT_ID="$VAPI_ASSISTANT_ID" VAPI_PHONE_NUMBER_ID="$VAPI_PHONE_NUMBER_ID" \
  VAPI_WEBHOOK_SECRET="$VAPI_WEBHOOK_SECRET" \
  APIFY_ENABLED="$APIFY_ENABLED" APIFY_API_TOKEN="$APIFY_API_TOKEN" \
  APIFY_DEFAULT_ACTOR_ID="$APIFY_DEFAULT_ACTOR_ID" \
    mvn -q spring-boot:run -Dspring-boot.run.jvmArguments="$JVM_ARGS"
) > "$ROOT/backend.log" 2>&1 &
BACK_PID=$!

echo "==> Waiting for backend health"
for _ in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/health 2>/dev/null || true)"
  [ "$code" = "200" ] && { echo "    backend UP"; break; }
  if ! kill -0 "$BACK_PID" 2>/dev/null; then
    echo "    backend process exited early - see backend.log"; break
  fi
  sleep 3
done

echo "==> Starting frontend ->  http://localhost:5173   (logs: frontend.log)"
(
  cd frontend
  [ -d node_modules ] || npm install
  npm run dev
) > "$ROOT/frontend.log" 2>&1 &
FRONT_PID=$!

cleanup() {
  echo; echo "==> Stopping backend + frontend..."
  kill "$BACK_PID" "$FRONT_PID" 2>/dev/null || true
  lsof -ti:8080 2>/dev/null | xargs -r kill -9 2>/dev/null || true
  lsof -ti:5173 2>/dev/null | xargs -r kill -9 2>/dev/null || true
  echo "   (Docker still running - use ./stop.sh to stop Postgres/Redis too)"
}
trap cleanup INT TERM

cat <<EOF

============================================================
  AgentOps CRM is starting:
    Frontend : http://localhost:5173
    Backend  : http://localhost:8080/api/health
    Postgres : localhost:5433    Redis: localhost:6379
  Logs: backend.log , frontend.log
  Press Ctrl+C to stop backend + frontend.
============================================================

EOF

# Stream both logs until Ctrl+C.
tail -f backend.log frontend.log
