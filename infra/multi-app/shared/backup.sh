#!/usr/bin/env bash
# Dumps every database on the shared Postgres, one file each.
#
#   ./backup.sh /var/backups/postgres
#
# Put it on a timer and copy the output OFF the machine. A backup that only exists on
# the server it protects is not a backup - it is a second copy of the same disk.
#
#   0 3 * * *  /home/you/shared/backup.sh /var/backups/postgres >> /var/log/pg-backup.log 2>&1

set -euo pipefail

DEST="${1:-}"
KEEP_DAYS="${KEEP_DAYS:-14}"

if [[ -z "$DEST" ]]; then
  echo "usage: $0 <destination-directory>" >&2
  exit 1
fi

cd "$(dirname "$0")"
mkdir -p "$DEST"

SUPERUSER="${POSTGRES_SUPERUSER:-postgres}"
STAMP="$(date +%Y%m%d-%H%M%S)"

# Every database except the templates and the maintenance one.
DATABASES="$(docker compose exec -T postgres psql -tAX -U "$SUPERUSER" \
  -c "SELECT datname FROM pg_database WHERE datistemplate = false AND datname <> 'postgres';")"

for DB in $DATABASES; do
  OUT="${DEST}/${DB}-${STAMP}.sql.gz"
  # Written to a .partial first and moved on success, so a dump interrupted halfway
  # never looks like a finished backup.
  docker compose exec -T postgres pg_dump -U "$SUPERUSER" --clean --if-exists "$DB" \
    | gzip > "${OUT}.partial"
  mv "${OUT}.partial" "$OUT"
  echo "$(date -Is)  wrote $OUT ($(du -h "$OUT" | cut -f1))"
done

# Prune old dumps. Runs last, so a failed backup above never deletes a good older one.
find "$DEST" -name '*.sql.gz' -mtime "+${KEEP_DAYS}" -print -delete
