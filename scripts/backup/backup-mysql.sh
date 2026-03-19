#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-deploy/.env}"
BACKUP_ROOT="${BACKUP_ROOT:-deploy/backups/mysql}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"

timestamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_ROOT"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

backup_file="$BACKUP_ROOT/mysql-${MYSQL_DATABASE}-${timestamp}.sql.gz"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T "$MYSQL_SERVICE" \
  mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  --single-transaction --quick --routines --triggers --events "$MYSQL_DATABASE" | gzip > "$backup_file"

find "$BACKUP_ROOT" -type f -name '*.sql.gz' -mtime +"$RETENTION_DAYS" -delete

echo "MySQL backup created: $backup_file"


