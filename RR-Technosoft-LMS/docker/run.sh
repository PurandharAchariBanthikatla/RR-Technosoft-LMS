#!/usr/bin/env bash
#
# RR Technosoft LMS — plain-Docker stack runner (replaces docker-compose.yml).
#
# Builds and starts postgres, redis, backend, and frontend as individually
# managed containers on a dedicated bridge network, wired together the same
# way the old docker-compose.yml did (same env vars, same ports, same
# healthcheck gating). Run with --monitoring to also start prometheus +
# grafana (replaces docker-compose.monitoring.yml).
#
# Usage:
#   cp .env.example .env   # fill in real values first
#   ./docker/run.sh                 # app stack only: postgres, redis, backend, frontend
#   ./docker/run.sh --monitoring    # app stack + prometheus, grafana
#
# Idempotent: safe to re-run. Existing containers/volumes/networks are reused.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ENV_FILE="${ENV_FILE:-.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy .env.example to .env and fill in real values first." >&2
  exit 1
fi
set -o allexport
# shellcheck disable=SC1090
source "$ENV_FILE"
set +o allexport

: "${DB_PASSWORD:?set DB_PASSWORD in .env}"
: "${JWT_SECRET:?set a 256-bit+ JWT_SECRET in .env}"
: "${SUPER_ADMIN_PASSWORD:?set SUPER_ADMIN_PASSWORD in .env}"

DB_NAME="${DB_NAME:-rr_lms}"
DB_USERNAME="${DB_USERNAME:-lms_user}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
NETWORK="rr-technosoft-lms_lms-net"
PG_VOLUME="rr-technosoft-lms_pgdata"
PROM_VOLUME="rr-technosoft-lms_prometheus-data"
GRAFANA_VOLUME="rr-technosoft-lms_grafana-data"
ALERTMANAGER_VOLUME="rr-technosoft-lms_alertmanager-data"

WITH_MONITORING=false
if [[ "${1:-}" == "--monitoring" ]]; then
  WITH_MONITORING=true
fi

log() { echo "[run.sh] $*"; }

log "Ensuring network '$NETWORK' exists"
docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK"

log "Ensuring volume '$PG_VOLUME' exists"
docker volume inspect "$PG_VOLUME" >/dev/null 2>&1 || docker volume create "$PG_VOLUME"

wait_healthy() {
  local name="$1" retries="${2:-30}" delay="${3:-2}"
  for ((i = 1; i <= retries; i++)); do
    status="$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "starting")"
    if [[ "$status" == "healthy" ]]; then
      log "$name is healthy"
      return 0
    fi
    sleep "$delay"
  done
  echo "[run.sh] $name did not become healthy in time — check 'docker logs $name'" >&2
  exit 1
}

# ---------------------------------------------------------------------------
# postgres
# ---------------------------------------------------------------------------
log "Starting postgres"
docker rm -f rr-lms-postgres >/dev/null 2>&1 || true
docker run -d \
  --name rr-lms-postgres \
  --restart unless-stopped \
  --network "$NETWORK" \
  -e POSTGRES_DB="$DB_NAME" \
  -e POSTGRES_USER="$DB_USERNAME" \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -v "$PG_VOLUME":/var/lib/postgresql/data \
  -p 5432:5432 \
  --health-cmd="pg_isready -U $DB_USERNAME -d $DB_NAME" \
  --health-interval=5s --health-timeout=5s --health-retries=10 \
  postgres:16-alpine

# ---------------------------------------------------------------------------
# redis
# ---------------------------------------------------------------------------
log "Starting redis"
docker rm -f rr-lms-redis >/dev/null 2>&1 || true
docker run -d \
  --name rr-lms-redis \
  --restart unless-stopped \
  --network "$NETWORK" \
  -p 6379:6379 \
  --health-cmd="redis-cli -a \"$REDIS_PASSWORD\" ping" \
  --health-interval=5s --health-timeout=5s --health-retries=10 \
  redis:7-alpine redis-server --requirepass "$REDIS_PASSWORD"

wait_healthy rr-lms-postgres
wait_healthy rr-lms-redis

# ---------------------------------------------------------------------------
# backend
# ---------------------------------------------------------------------------
log "Building backend image"
docker build -t rr-lms-backend:local ./backend

log "Starting backend"
docker rm -f rr-lms-backend >/dev/null 2>&1 || true
docker run -d \
  --name rr-lms-backend \
  --restart unless-stopped \
  --network "$NETWORK" \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}" \
  -e DB_URL="jdbc:postgresql://rr-lms-postgres:5432/$DB_NAME" \
  -e DB_USERNAME="$DB_USERNAME" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e REDIS_HOST="rr-lms-redis" \
  -e REDIS_PORT="6379" \
  -e REDIS_PASSWORD="$REDIS_PASSWORD" \
  -e MANAGEMENT_HEALTH_MAIL_ENABLED="false" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e JWT_ACCESS_EXPIRY="${JWT_ACCESS_EXPIRY:-900000}" \
  -e JWT_REFRESH_EXPIRY="${JWT_REFRESH_EXPIRY:-604800000}" \
  -e CORS_ORIGINS="${CORS_ORIGINS:-http://localhost:3000}" \
  -e SMTP_HOST="${SMTP_HOST:-}" \
  -e SMTP_PORT="${SMTP_PORT:-587}" \
  -e SMTP_USERNAME="${SMTP_USERNAME:-}" \
  -e SMTP_PASSWORD="${SMTP_PASSWORD:-}" \
  -e MAIL_FROM_ADDRESS="${MAIL_FROM_ADDRESS:-no-reply@rrtechnosoft.com}" \
  -e MAIL_FROM_NAME="${MAIL_FROM_NAME:-RR Technosoft LMS}" \
  -e WHATSAPP_ENABLED="${WHATSAPP_ENABLED:-false}" \
  -e TWILIO_ACCOUNT_SID="${TWILIO_ACCOUNT_SID:-}" \
  -e TWILIO_AUTH_TOKEN="${TWILIO_AUTH_TOKEN:-}" \
  -e TWILIO_WHATSAPP_FROM="${TWILIO_WHATSAPP_FROM:-}" \
  -e RAZORPAY_ENABLED="${RAZORPAY_ENABLED:-false}" \
  -e RAZORPAY_KEY_ID="${RAZORPAY_KEY_ID:-}" \
  -e RAZORPAY_KEY_SECRET="${RAZORPAY_KEY_SECRET:-}" \
  -e RAZORPAY_WEBHOOK_SECRET="${RAZORPAY_WEBHOOK_SECRET:-}" \
  -e CHATBOT_ENABLED="${CHATBOT_ENABLED:-false}" \
  -e CHATBOT_API_KEY="${CHATBOT_API_KEY:-}" \
  -e CHATBOT_BASE_URL="${CHATBOT_BASE_URL:-https://api.openai.com/v1}" \
  -e CHATBOT_MODEL="${CHATBOT_MODEL:-gpt-4o-mini}" \
  -e S3_BUCKET="${S3_BUCKET:-}" \
  -e AWS_REGION="${AWS_REGION:-ap-south-1}" \
  -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-}" \
  -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-}" \
  -e SEED_DATA="${SEED_DATA:-true}" \
  -e SEED_DEMO_DATA="${SEED_DEMO_DATA:-false}" \
  -e SUPER_ADMIN_EMAIL="${SUPER_ADMIN_EMAIL:-superadmin@rrtechnosoft.com}" \
  -e SUPER_ADMIN_PASSWORD="$SUPER_ADMIN_PASSWORD" \
  -p 8081:8080 \
  --health-cmd="wget -qO- http://localhost:8080/api/v1/actuator/health || exit 1" \
  --health-interval=10s --health-timeout=5s --health-retries=10 --health-start-period=30s \
  rr-lms-backend:local

wait_healthy rr-lms-backend 60 5

# ---------------------------------------------------------------------------
# frontend
# ---------------------------------------------------------------------------
FRONTEND_API_BASE_URL="${FRONTEND_API_BASE_URL:-http://localhost:8081/api/v1}"
log "Building frontend image (NEXT_PUBLIC_API_BASE_URL=$FRONTEND_API_BASE_URL)"
docker build \
  --build-arg NEXT_PUBLIC_API_BASE_URL="$FRONTEND_API_BASE_URL" \
  -t rr-lms-frontend:local ./rr-technosoft-lms

log "Starting frontend"
docker rm -f rr-lms-frontend >/dev/null 2>&1 || true
docker run -d \
  --name rr-lms-frontend \
  --restart unless-stopped \
  --network "$NETWORK" \
  -p 3000:3000 \
  rr-lms-frontend:local

if [[ "$WITH_MONITORING" == "true" ]]; then
  log "Ensuring monitoring volumes exist"
  docker volume inspect "$PROM_VOLUME" >/dev/null 2>&1 || docker volume create "$PROM_VOLUME"
  docker volume inspect "$GRAFANA_VOLUME" >/dev/null 2>&1 || docker volume create "$GRAFANA_VOLUME"
  docker volume inspect "$ALERTMANAGER_VOLUME" >/dev/null 2>&1 || docker volume create "$ALERTMANAGER_VOLUME"

  : "${GRAFANA_ADMIN_PASSWORD:?set GRAFANA_ADMIN_PASSWORD in .env}"

  log "Starting postgres-exporter"
  docker rm -f rr-lms-postgres-exporter >/dev/null 2>&1 || true
  docker run -d \
    --name rr-lms-postgres-exporter \
    --restart unless-stopped \
    --network "$NETWORK" \
    -e DATA_SOURCE_URI="rr-lms-postgres:5432/$DB_NAME?sslmode=disable" \
    -e DATA_SOURCE_USER="$DB_USERNAME" \
    -e DATA_SOURCE_PASS="$DB_PASSWORD" \
    -p 9187:9187 \
    quay.io/prometheuscommunity/postgres-exporter:v0.15.0

  log "Starting redis-exporter"
  docker rm -f rr-lms-redis-exporter >/dev/null 2>&1 || true
  docker run -d \
    --name rr-lms-redis-exporter \
    --restart unless-stopped \
    --network "$NETWORK" \
    -e REDIS_ADDR="redis://rr-lms-redis:6379" \
    -e REDIS_PASSWORD="$REDIS_PASSWORD" \
    -p 9121:9121 \
    oliver006/redis_exporter:v1.62.0

  log "Starting alertmanager"
  docker rm -f rr-lms-alertmanager >/dev/null 2>&1 || true
  docker run -d \
    --name rr-lms-alertmanager \
    --restart unless-stopped \
    --network "$NETWORK" \
    -v "$ROOT_DIR/monitoring/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro" \
    -v "$ALERTMANAGER_VOLUME":/alertmanager \
    -p 9093:9093 \
    prom/alertmanager:v0.27.0 \
    --config.file=/etc/alertmanager/alertmanager.yml \
    --storage.path=/alertmanager

  log "Starting prometheus"
  docker rm -f rr-lms-prometheus >/dev/null 2>&1 || true
  docker run -d \
    --name rr-lms-prometheus \
    --restart unless-stopped \
    --network "$NETWORK" \
    -v "$ROOT_DIR/monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
    -v "$ROOT_DIR/monitoring/prometheus/alert.rules.yml:/etc/prometheus/alert.rules.yml:ro" \
    -v "$PROM_VOLUME":/prometheus \
    -p 9090:9090 \
    prom/prometheus:v2.55.1 \
    --config.file=/etc/prometheus/prometheus.yml \
    --storage.tsdb.path=/prometheus \
    --storage.tsdb.retention.time=15d

  log "Starting grafana"
  docker rm -f rr-lms-grafana >/dev/null 2>&1 || true
  docker run -d \
    --name rr-lms-grafana \
    --restart unless-stopped \
    --network "$NETWORK" \
    -e GF_SECURITY_ADMIN_USER="${GRAFANA_ADMIN_USER:-admin}" \
    -e GF_SECURITY_ADMIN_PASSWORD="$GRAFANA_ADMIN_PASSWORD" \
    -e GF_USERS_ALLOW_SIGN_UP="false" \
    -v "$ROOT_DIR/monitoring/grafana/provisioning:/etc/grafana/provisioning:ro" \
    -v "$GRAFANA_VOLUME":/var/lib/grafana \
    -p 3001:3000 \
    grafana/grafana:11.2.2
fi

log "Stack is up: frontend http://localhost:3000  backend http://localhost:8081/api/v1"
if [[ "$WITH_MONITORING" == "true" ]]; then
  log "Prometheus http://localhost:9090  Grafana http://localhost:3001  Alertmanager http://localhost:9093"
fi
