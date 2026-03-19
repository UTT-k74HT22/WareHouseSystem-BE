#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/whs}"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_ROOT/deploy/docker-compose.prod.yml}"
MONITORING_COMPOSE_FILE="${MONITORING_COMPOSE_FILE:-$DEPLOY_ROOT/deploy/docker-compose.monitoring.yml}"
ENV_FILE="${ENV_FILE:-$DEPLOY_ROOT/deploy/.env}"
APP_IMAGE="${APP_IMAGE:-}"
ENABLE_MONITORING="${ENABLE_MONITORING:-false}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:8080/actuator/health}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-10}"

if [[ -z "$APP_IMAGE" ]]; then
  echo "APP_IMAGE is required" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing compose file: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

if [[ "$ENABLE_MONITORING" == "true" ]] && [[ ! -f "$MONITORING_COMPOSE_FILE" ]]; then
  echo "Missing monitoring compose file: $MONITORING_COMPOSE_FILE" >&2
  exit 1
fi

mkdir -p \
  "$DEPLOY_ROOT/deploy/certs" \
  "$DEPLOY_ROOT/deploy/data/mysql" \
  "$DEPLOY_ROOT/deploy/data/redis" \
  "$DEPLOY_ROOT/deploy/data/rabbitmq" \
  "$DEPLOY_ROOT/deploy/data/minio" \
  "$DEPLOY_ROOT/deploy/data/email-attachments" \
  "$DEPLOY_ROOT/deploy/logs/app" \
  "$DEPLOY_ROOT/deploy/logs/nginx" \
  "$DEPLOY_ROOT/deploy/monitoring/prometheus/data" \
  "$DEPLOY_ROOT/deploy/monitoring/grafana/data"

cd "$DEPLOY_ROOT"

if grep -q '^APP_IMAGE=' "$ENV_FILE"; then
  sed -i "s|^APP_IMAGE=.*$|APP_IMAGE=$APP_IMAGE|" "$ENV_FILE"
else
  printf '\nAPP_IMAGE=%s\n' "$APP_IMAGE" >> "$ENV_FILE"
fi

compose_args=(--env-file "$ENV_FILE" -f "$COMPOSE_FILE")
if [[ "$ENABLE_MONITORING" == "true" ]]; then
  compose_args+=(-f "$MONITORING_COMPOSE_FILE")
fi

docker compose "${compose_args[@]}" pull
docker compose "${compose_args[@]}" up -d --remove-orphans

attempt=1
until docker compose "${compose_args[@]}" exec -T app \
  sh -lc "curl --fail --silent '$HEALTHCHECK_URL' >/dev/null"; do
  if (( attempt >= MAX_ATTEMPTS )); then
    echo "Application health check failed after $MAX_ATTEMPTS attempts" >&2
    docker compose "${compose_args[@]}" ps >&2
    docker compose "${compose_args[@]}" logs --tail=200 app >&2
    exit 1
  fi

  echo "Waiting for application health check... attempt $attempt/$MAX_ATTEMPTS"
  sleep "$SLEEP_SECONDS"
  attempt=$((attempt + 1))
done

docker compose "${compose_args[@]}" ps
echo "Deployment completed successfully with image: $APP_IMAGE"
