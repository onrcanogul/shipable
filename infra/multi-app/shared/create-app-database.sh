#!/usr/bin/env bash
# Creates a database and a user for one app on the shared Postgres.
#
#   ./create-app-database.sh app_one
#
# Prints the generated password once. Put it straight into that app's .env as
# APP_DB_PASSWORD - it is not stored anywhere else and cannot be recovered, only reset.
#
# Separate databases rather than separate schemas, because this template names its
# schemas identity, billing, quota... which would collide between apps, and because a
# `flyway clean` or a restore must not be able to reach another app's data.

set -euo pipefail

APP="${1:-}"
if [[ -z "$APP" ]]; then
  echo "usage: $0 <app_name>    (lowercase, underscores; e.g. app_one)" >&2
  exit 1
fi

if [[ ! "$APP" =~ ^[a-z][a-z0-9_]{1,62}$ ]]; then
  # Postgres would fold or reject anything else, and an app that cannot connect because
  # of a capital letter is a bad hour.
  echo "error: '$APP' must be lowercase letters, digits and underscores, starting with a letter" >&2
  exit 1
fi

cd "$(dirname "$0")"

PASSWORD="$(openssl rand -base64 24)"

# Runs as the superuser inside the container; nothing is exposed off the machine.
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "${POSTGRES_SUPERUSER:-postgres}" <<SQL
CREATE USER ${APP} WITH PASSWORD '${PASSWORD}';
CREATE DATABASE ${APP} OWNER ${APP};
-- The app owns its database and creates its own schemas through Flyway. It gets nothing
-- in anyone else's database, which is the entire point of separating them.
REVOKE ALL ON DATABASE ${APP} FROM PUBLIC;
GRANT ALL PRIVILEGES ON DATABASE ${APP} TO ${APP};
SQL

cat <<INFO

Created database '${APP}' owned by user '${APP}'.

Put these in that app's infra/multi-app/app/.env:

  APP_DB_URL=jdbc:postgresql://postgres:5432/${APP}
  APP_DB_USERNAME=${APP}
  APP_DB_PASSWORD=${PASSWORD}

The password is shown once. It is not written to disk here.
INFO
