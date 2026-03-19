#!/usr/bin/env bash
set -euo pipefail

MINIO_DATA_DIR="${MINIO_DATA_DIR:-deploy/data/minio}"
BACKUP_ROOT="${BACKUP_ROOT:-deploy/backups/minio}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

timestamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_ROOT"

if [[ ! -d "$MINIO_DATA_DIR" ]]; then
  echo "Missing MinIO data directory: $MINIO_DATA_DIR" >&2
  exit 1
fi

backup_file="$BACKUP_ROOT/minio-${timestamp}.tar.gz"
tar -C "$MINIO_DATA_DIR" -czf "$backup_file" .

find "$BACKUP_ROOT" -type f -name '*.tar.gz' -mtime +"$RETENTION_DAYS" -delete

echo "MinIO backup created: $backup_file"

