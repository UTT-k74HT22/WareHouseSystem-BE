#!/usr/bin/env bash
set -euo pipefail

RESTORE_ROOT="${RESTORE_ROOT:-.}"
BACKUP_FILE="${1:-}"

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Usage: $0 <path-to-config-backup.tar.gz>" >&2
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

echo "Restoring config backup from $BACKUP_FILE into $RESTORE_ROOT"
tar -xzf "$BACKUP_FILE" -C "$RESTORE_ROOT"

echo "Config restore completed."
