#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$SCRIPT_DIR/backup-mysql.sh"
"$SCRIPT_DIR/backup-minio.sh"
"$SCRIPT_DIR/backup-config.sh"

echo "All backups completed successfully."
