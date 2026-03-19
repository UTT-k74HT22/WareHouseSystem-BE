#!/usr/bin/env bash
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-deploy/backups/config}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

timestamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_ROOT"

backup_file="$BACKUP_ROOT/config-${timestamp}.tar.gz"

tar -czf "$backup_file" \
  deploy/docker-compose.prod.yml \
  deploy/.env \
  deploy/nginx \
  scripts/backup

find "$BACKUP_ROOT" -type f -name '*.tar.gz' -mtime +"$RETENTION_DAYS" -delete

echo "Config backup created: $backup_file"
