#!/bin/sh
# RR Technosoft LMS — database restore script.
#
# Restores a .sql or .sql.gz dump (as produced by backup.sh or the in-app
# BackupService) into the target database. DESTRUCTIVE: this drops and
# recreates the public schema before restoring, so it will erase whatever
# is currently in DB_NAME. Intended for disaster recovery, not routine use.
#
# POSIX /bin/sh only (no bashisms) so this also runs unmodified inside
# minimal containers (e.g. postgres:*-alpine) that don't ship bash.
#
# Required env vars: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
# Usage: ./scripts/restore.sh /path/to/rr-lms-backup-20260802-030000.sql.gz
#        ./scripts/restore.sh /path/to/dump.sql --yes   (skip confirmation, for CI/DR automation)

set -eu

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:=5432}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

DUMP_FILE="${1:?Usage: restore.sh <dump-file> [--yes]}"
SKIP_CONFIRM="${2:-}"

if [ ! -f "$DUMP_FILE" ]; then
  echo "[restore] ERROR: file not found: $DUMP_FILE" >&2
  exit 1
fi

if [ "$SKIP_CONFIRM" != "--yes" ]; then
  echo "This will PERMANENTLY ERASE all data in '${DB_NAME}' on ${DB_HOST}:${DB_PORT}"
  echo "and replace it with the contents of: ${DUMP_FILE}"
  printf "Type the database name (%s) to confirm: " "$DB_NAME"
  read -r CONFIRM
  if [ "$CONFIRM" != "$DB_NAME" ]; then
    echo "[restore] Confirmation did not match — aborting." >&2
    exit 1
  fi
fi

export PGPASSWORD="$DB_PASSWORD"

echo "[restore] Dropping and recreating the public schema on ${DB_NAME}..."
psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" --dbname="$DB_NAME" \
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

echo "[restore] Restoring from ${DUMP_FILE}..."
case "$DUMP_FILE" in
  *.gz)
    gunzip -c "$DUMP_FILE" | psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" --dbname="$DB_NAME"
    ;;
  *)
    psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" --dbname="$DB_NAME" -f "$DUMP_FILE"
    ;;
esac

unset PGPASSWORD

echo "[restore] Done. Recommended next steps:"
echo "  1. Run 'mvn -f backend flyway:info' to confirm the schema_version table matches the app's expected migrations."
echo "  2. Smoke-test login and a few core screens before pointing production traffic at this database."
