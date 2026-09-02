# platform/admin

The operator API: change how the app behaves without a deploy.

**This is the most dangerous surface in the application.** It can throttle every caller,
lock out every client below a version, and put the app into maintenance. It is off by
default, refuses to start without a real key, and is blocked at the edge by Caddy.

## Endpoints

All under `/api/admin/v1`, all requiring `X-Admin-Key`.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/settings` | Every setting: type, current value, boot default, whether overridden |
| `GET` | `/settings/{key}` | One setting |
| `PUT` | `/settings/{key}` | Override it (validated against its declared type) |
| `DELETE` | `/settings/{key}` | Drop the override, back to the boot default |
| `GET` | `/feature-flags` | Every flag |
| `PUT` | `/feature-flags/{key}` | Create or flip a flag |
| `DELETE` | `/feature-flags/{key}` | Delete a flag |
| `GET` | `/platform-config` | Version gating per platform |
| `PUT` | `/platform-config/{platform}` | Set minimum and latest version |
| `GET` | `/info` | Active profile, version, uptime |

## What you can change at runtime

| Key | Type | What it does |
| --- | --- | --- |
| `core.rate-limit.enabled` | boolean | Per-IP rate limiting on/off |
| `core.rate-limit.permits` | integer | Requests per window per IP |
| `core.rate-limit.window` | duration | The window, e.g. `1m` |
| `appconfig.version-gate.enabled` | boolean | The force-update gate's off switch |
| `appconfig.maintenance.enabled` | boolean | Tell clients to show a maintenance screen |
| `appconfig.maintenance.message` | string | What that screen says |
| `cache.bypass` | boolean | Cache off without dropping the Redis connection |
| `cache.default-ttl` | duration | TTL for cache writes that do not name one |

Your app adds its own by publishing a `SettingCatalog` bean from `domain`; they appear in
the listing with no change here.

## Using it

    export KEY=...   # APP_ADMIN_API_KEY
    curl -H "X-Admin-Key: $KEY" http://localhost:8080/api/admin/v1/settings

    curl -X PUT -H "X-Admin-Key: $KEY" -H 'Content-Type: application/json' \
      -d '{"value":"30","note":"someone is hammering /auth"}' \
      http://localhost:8080/api/admin/v1/settings/core.rate-limit.permits

In production Caddy 404s `/api/admin/*`, so reach it through an SSH tunnel:

    ssh -L 8080:localhost:8080 user@server

## Tables it owns

None. Settings, flags and version config all live in `appconfig`; this module is the
operator interface to them, not their owner.

## Interfaces it exposes

None. Nothing depends on this module — it is a leaf, and deleting it removes the API and
nothing else.

Spring wiring: `AdminModuleConfiguration`.

## Decisions worth knowing

- **Off by default**, and off by default in `prod`. An endpoint that does not exist cannot
  be attacked.
- **A weak key stops startup.** Not a warning — a warning about an unprotected admin API
  scrolls past.
- **Constant-time key comparison.** String equality returns on the first differing byte,
  leaking the key one character at a time to anyone willing to measure. On an endpoint an
  attacker can call freely, that is a real attack.
- **Optional IP allowlist**, so a leaked key is not enough on its own. Only meaningful
  behind a proxy that sets `X-Forwarded-For`, which Caddy does.
- **The guard runs before authentication and before the version gate.** An operator must be
  able to reach this API while the app is in maintenance or turning every client away —
  those are exactly the moments they need it.
- **Only catalogued keys can be written**, and values are validated against the declared
  type. Otherwise the settings table fills with typos that silently do nothing.
- **Every change is logged at INFO** with the caller's IP and an optional note.
- **`/info` reports the active profile and version, and nothing else from the environment.**
  Dumping every property would be convenient about half the time; the other half you have
  published your database password.

## What it explicitly does NOT do

- **No user management.** No listing users, no impersonation, no refunds. Those need real
  admin accounts with an audit trail, not a shared key.
- **No identity, only a key.** The audit trail records `admin-key@<ip>`. If you need to
  know *who*, give each operator their own key or move to admin accounts.
- **No UI.** JSON only. Point Postman or a small internal page at it.
- **No rollback or change history.** The current value and who last set it, nothing more.
- **No writes to anything but settings, flags and version config.** It cannot touch user
  data, and it should stay that way.
