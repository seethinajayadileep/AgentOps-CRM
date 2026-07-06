#!/usr/bin/env bash
#
# AgentOps CRM - stop the whole stack (backend + frontend + Docker containers).
#
# Usage:
#   ./stop.sh            # stop backend, frontend, and Postgres/Redis (keeps DB data)
#   ./stop.sh --wipe     # also delete the Postgres/Redis volumes (fresh DB next run)

set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Stopping backend (8080) and frontend (5173)"
lsof -ti:8080 2>/dev/null | xargs -r kill -9 2>/dev/null || true
lsof -ti:5173 2>/dev/null | xargs -r kill -9 2>/dev/null || true
pkill -f 'spring-boot:run' 2>/dev/null || true

if [ "${1:-}" = "--wipe" ]; then
  echo "==> Stopping Docker containers AND wiping volumes"
  docker compose -f docker/docker-compose.yml down -v
else
  echo "==> Stopping Docker containers (data preserved)"
  docker compose -f docker/docker-compose.yml down
fi

echo "Done."
