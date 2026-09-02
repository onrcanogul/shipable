# platform/appconfig

The answer to "can this client still be used, and what is turned on?" — asked before
sign-in.

## Responsibility

- Minimum supported version per platform, with force-update signalling.
- Maintenance mode, so an outage shows a screen instead of a spinner.
- Server-side feature flags.

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/config` | public | Everything the client needs at launch |

Public on purpose: a client that must update needs to find that out without being able to
authenticate.

## Tables it owns

| Schema | Table | Purpose |
| --- | --- | --- |
| `appconfig` | `platform_config` | Min/latest version, update URL, maintenance mode, per platform. |
| `appconfig` | `feature_flag` | Server-side switches. `exposed_to_client` decides what the app is told. |

Migrations: `src/main/resources/db/migration/appconfig/`

Seeded permissive (`0.0.0`), so an empty table cannot lock every client out.

## Interfaces it exposes

- `RemoteConfigService` — `configFor`, `isSupported`.
- `FeatureFlags` — `isEnabled(flag, default)`.
- `AppVersion` — comparable, so version checks are numeric.

Spring wiring: `AppConfigModuleConfiguration`.

## Decisions worth knowing

- **`AppVersion` is a type, not a string.** Comparing versions as strings puts `1.10.0`
  before `1.9.0`, and your force-update gate locks out the newest release. There is a test
  for exactly that.
- **Configuration lives in the database.** Raising the minimum version is something you do
  in a hurry because a shipped build is doing damage. Needing a redeploy is how the
  redeploy becomes the outage.
- **A missing version header means supported.** Rejecting unknown clients would lock out
  every caller that predates the header, including your own tooling.
- **The gate has an off switch.** `app.config.version-gate-enabled=false`. Anything that can
  lock out all your users should be switchable without a deploy.
- **426 Upgrade Required, not 400.** The client can tell "you must update" from "your
  request was wrong" without parsing a message.
- **`exposed_to_client` on flags.** Some flags decide what the app draws, others what the
  server does. Sending the second kind announces what you are about to launch.

## What it explicitly does NOT do

- **No database reads yet.** `DefaultRemoteConfigService` returns the permissive fallback;
  the query and its cache are TODO.
- **No flag evaluation yet.** `DatabaseFeatureFlags` returns the caller's default.
- **No per-user or percentage rollouts.** Flags are global on/off.
- **No admin API.** Change rows with SQL, or build an admin surface in `domain`.
