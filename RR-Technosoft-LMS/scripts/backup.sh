#!/bin/sh
# RR Technosoft LMS — standalone database backup script.
#
# This is deliberately independent of the running Spring Boot app (which has
# its own in-app scheduled backup via BackupScheduler/BackupService) so a
# disaster-recovery backup can still be taken even if the app itself is down.
# Suitable for a host cron job, a Jenkins pipeline step, or a Kubernetes
# CronJob (see k8s/base/backup-cronjob.yaml, which runs this under the
# postgres:16-alpine image — POSIX /bin/sh only, no bash, hence no bashisms
# like 'set -o pipefail' or 'read -p' anywhere in this file).
#
# Required env vars: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
# Optional: BACKUP_DIR (default /var/backups/rr-lms), RETENTION_DAYS (default 30)
#
# Usage: ./scripts/backup.sh

set -eu

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:=5432}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/rr-lms}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
DUMP_FILE="${BACKUP_DIR}/rr-lms-backup-${TIMESTAMP}.sql.gz"

echo "[backup] Starting dump of ${DB_NAME}@${DB_HOST}:${DB_PORT} -> ${DUMP_FILE}"

export PGPASSWORD="$DB_PASSWORD"
pg_dump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --format=plain \
  --no-owner \
  --no-privileges \
  "$DB_NAME" | gzip > "$DUMP_FILE"
unset PGPASSWORD

SIZE="$(du -h "$DUMP_FILE" | cut -f1)"
echo "[backup] Done: ${DUMP_FILE} (${SIZE})"

# Verify the dump isn't empty/truncated before trusting it.
if [ ! -s "$DUMP_FILE" ]; then
  echo "[backup] ERROR: dump file is empty — treating this backup as failed" >&2
  rm -f "$DUMP_FILE"
  exit 1
fi

echo "[backup] Pruning backups older than ${RETENTION_DAYS} days in ${BACKUP_DIR}"
find "$BACKUP_DIR" -name 'rr-lms-backup-*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -print -delete

echo "[backup] Complete."
