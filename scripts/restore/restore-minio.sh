#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-deploy/.env}"
MINIO_SERVICE="${MINIO_SERVICE:-minio}"
MINIO_DATA_DIR="${MINIO_DATA_DIR:-deploy/data/minio}"
BACKUP_FILE="${1:-}"

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Usage: $0 <path-to-minio-backup.tar.gz>" >&2
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

mkdir -p "$MINIO_DATA_DIR"

echo "Stopping MinIO service before restore"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" stop "$MINIO_SERVICE"

rm -rf "$MINIO_DATA_DIR"/*
tar -C "$MINIO_DATA_DIR" -xzf "$BACKUP_FILE"

echo "Starting MinIO service after restore"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d "$MINIO_SERVICE"

echo "MinIO restore completed."
