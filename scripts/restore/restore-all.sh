#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <mysql-backup.sql.gz> <minio-backup.tar.gz> <config-backup.tar.gz>" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$SCRIPT_DIR/restore-config.sh" "$3"
"$SCRIPT_DIR/restore-minio.sh" "$2"
"$SCRIPT_DIR/restore-mysql.sh" "$1"

echo "Full restore sequence completed."
