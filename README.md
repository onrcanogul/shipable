# app-backend-template

A Spring Boot backend starter for indie mobile apps. Everything an app needs before it does
anything interesting is here, already built and domain-agnostic:

- **Sign-in** — Sign in with Apple, Google Sign-In, anonymous device accounts, and
  anonymous-to-account linking, with session tokens this backend issues itself.
- **Subscriptions** — RevenueCat entitlements with an authenticated webhook and a local
  snapshot, so a paywall check is a local query.
- **Quotas** — entitlement-to-limit mapping and a usage ledger.
- **Push and e-mail** — device registry and sender ports, no vendor chosen yet.
- **Remote config** — minimum supported version, force update, maintenance mode, feature
  flags.
- **An admin API** — change rate limits, flip feature flags, raise the minimum version and
  enable maintenance at runtime, without a redeploy.
- **Redis** — optional; turn it on and rate limiting, idempotency and caching become shared
  across instances.
- **Account deletion and data export** — the two compliance obligations the app stores
  actually check.
- **The unglamorous parts** — one error shape, request ids in every log line, validation,
  per-IP rate limiting, `Idempotency-Key` handling.

Clone it, write your app in `domain`, and never build this layer again.

## Quick start

    git clone <this repo> my-app && cd my-app
    cd infra && cp .env.example .env
    # set APP_JWT_SECRET: openssl rand -base64 48
    docker compose up --build

    # with Redis:
    # APP_CACHE_ENABLED=true in .env, then
    docker compose --profile redis up --build

- API — https://localhost/api/v1/health
- Swagger UI — https://localhost/swagger-ui.html

Without Docker:

    ./mvnw verify
    APP_JWT_SECRET=$(openssl rand -base64 48) ./mvnw -pl host spring-boot:run

## Requirements

Java 21, Docker (optional locally; needed for the integration test and for running the
stack). Maven comes from the wrapper.

## Layout

    platform/     reusable modules; none of them know what your app does
      core/           request pipeline, errors, base entity, rate limit, idempotency
      cache/          Redis (optional): cache, shared rate limiting and idempotency
      identity/       sign-in and sessions
      billing/        RevenueCat
      quota/          limits and usage
      notifications/  device tokens, push and e-mail ports
      analytics/      event port
      appconfig/      version gating, maintenance, feature flags
      privacy/        account deletion and data export
      admin/          operator API under /api/admin/v1
    domain/       YOUR APP. Ships empty.
    host/         the Spring Boot application
    infra/        docker compose, Caddy, .env.example
    docs/

Each module has its own README covering what it does, what tables it owns, what it exposes,
and — explicitly — what it does not do.

## Read next

| Document | For |
| --- | --- |
| [docs/BUILDING-YOUR-APP.md](docs/BUILDING-YOUR-APP.md) | Rename it, write your first feature, ship |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | The rules and why they are there |
| [infra/README.md](infra/README.md) | Running it locally and on a VPS |

## Honest status

This is a **skeleton with real boundaries**, not a finished backend. The structure,
configuration, error handling, filters, schema, JWT issuing and architecture tests are
done. The integrations are not:

| Works today | Still a TODO |
| --- | --- |
| Request pipeline, errors, validation | Apple/Google token verification |
| JWT issue/validate, refresh hashing | Sign-in persistence, refresh rotation |
| Rate limiting and idempotency, in memory **and** on Redis | — |
| Runtime settings: read, write, validate, refresh | — |
| Admin API: settings, flags, version gating, info | Admin UI, user management |
| Feature flags and version gating, read from the database | Percentage rollouts |
| Webhook authentication, 202-then-process | RevenueCat API calls and event processing |
| Quota wiring, entitlement lookup | Rolling-window accounting, ledger writes |
| Data export fan-out | Deletion execution and its sweep job |
| Migrations, Docker, Caddy, dev/prod profiles, CI/CD | Push/e-mail providers, backups |

Every TODO sits in the code with notes on how to do it and which mistakes to avoid.
`docs/BUILDING-YOUR-APP.md` has the full checklist.

The design principle behind the gaps: **an unimplemented feature denies rather than
allows**. Nobody is entitled to anything, no quota passes, and token verification throws
rather than returning a "verified" identity. A stub that silently succeeds is how a template
becomes a security incident.
