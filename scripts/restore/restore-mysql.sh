#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-deploy/.env}"
MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"
BACKUP_FILE="${1:-}"

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Usage: $0 <path-to-mysql-backup.sql.gz>" >&2
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

echo "Restoring MySQL backup from $BACKUP_FILE"
gunzip -c "$BACKUP_FILE" | docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T "$MYSQL_SERVICE" \
  mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"

echo "MySQL restore completed."
