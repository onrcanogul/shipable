# Architecture

A backend starter for indie mobile apps. Everything an app needs before it does anything
interesting — sign-in, subscriptions, quotas, push, config, account deletion — is here and
domain-agnostic. You clone it, write your app in `domain`, and never build this layer
again.

## Shape

    app-backend-template/
      pom.xml            parent: versions, plugins, module list
      platform/          reusable modules; none of them know what your app does
        core/            request pipeline, errors, base entity, rate limit, idempotency
        cache/           Redis, optional: cache, shared rate limiting, shared idempotency
        identity/        Apple/Google sign-in, anonymous devices, our own JWT sessions
        billing/         RevenueCat entitlements and webhooks
        quota/           entitlement-to-limit mapping and the usage ledger
        notifications/   device registry, push and e-mail ports
        analytics/       product event port
        appconfig/       version gating, maintenance mode, feature flags
        privacy/         account deletion and data export
        admin/           operator API under /api/admin/v1
      domain/            YOUR APP. Ships empty.
      host/              the Spring Boot application; wires everything under /api/v1
      infra/             docker compose, Caddyfile, .env.example
      docs/

## The rules

### 1. The platform never depends on your app

`platform/* -> domain` is forbidden. That single direction is what makes this a template:
delete `domain`, and the platform still compiles and still runs.

Enforced twice — the Maven enforcer in `platform/pom.xml` fails the build on a declared
dependency, and `ArchitectureRulesTest` fails on an actual import.

### 2. A module's `internal` package belongs to that module

Every module is:

    <module>/
      <Module>ModuleConfiguration.java   the only Spring entry point
      api/
        model/    value objects and enums
        port/     interfaces: what it offers, and what it needs from elsewhere
        dto/      request/response payloads
      internal/
        service/                implementations
        persistence/entity/     JPA entities
        persistence/repository/ Spring Data repositories
        web/                    controllers
        client/                 outbound HTTP
        support/                module-private helpers

Only `api` is public. `internal` is enforced closed by ArchUnit.

### 3. Wiring is explicit, never scanned

The host imports each module's configuration **by name**. Nothing component-scans a
module's internals. So `Application` is the complete answer to "what is running?", and
`<Module>ModuleConfiguration` is the complete answer to "what does this module contribute?".
No bean ever appears because a package happened to be scanned.

### 4. Configuration comes from the environment

`application.yml` reads every secret as `${VAR}` with **no default**, so a missing secret
stops the application at startup rather than surfacing later as a strange 500. The only
secrets file in the repository is `infra/.env.example`, which contains no secrets.

### 5. One error shape

Every failure from every endpoint is a `ProblemBody`: `status`, `code`, `message`,
`requestId`, optional `fieldErrors`. Clients branch on `code`, which is stable; `message` is
not. Filters use `ProblemResponseWriter` so they cannot drift into a second format.

### 6. Deletion is opt-in by the module, not by memory

A module that stores user data publishes a `UserDataContributor` bean. `privacy` collects
every one of them. A module added next year joins the deletion and export flows by
publishing a bean — not by someone remembering a checklist.

### 7. Safe defaults are the ones that deny

- No entitlement recorded → no entitlement.
- No quota limit configured → denied.
- Token verification unimplemented → throws, never returns a "verified" identity.
- Webhook without the shared secret → 401.

The one deliberate exception is `appconfig`: with no configuration, every client is
supported. A version gate that fails closed takes down the whole app the first time
someone drops a table, which is the opposite of a safety valve.

## Request pipeline

| Order | Filter | Module | Job |
| --- | --- | --- | --- |
| +10 | `RequestContextFilter` | core | Request id, platform, version, IP; MDC |
| +20 | `RateLimitFilter` | core | Per-IP flood control, before anything costs money |
| +30 | `IdempotencyFilter` | core | `Idempotency-Key` on state-changing requests |
| +15 | `AdminAuthenticationFilter` | admin | Key and IP check on `/api/admin/**` |
| +35 | `MinimumVersionFilter` | appconfig | 426 for clients below the minimum |
| +40 | `JwtAuthenticationFilter` | identity | Bind the caller if a valid token is present |

Authentication is **permissive**; endpoints are **strict**. The filter binds a user when
there is one and otherwise does nothing; endpoints call `CurrentUserHolder.require()` or
`requireRegistered()`. A new endpoint is therefore closed by default rather than public
because someone forgot to add it to a path list.

## Module dependencies

    core  ← identity ← billing ← quota
      ↑        ↑
      |        ├── notifications
      |        └── privacy
      ├── cache
      ├── analytics
      └── appconfig ← admin

    domain → every platform module except cache and admin
    host   → domain, privacy, cache, admin (and everything transitively)

No cycles. `core` depends on nothing. Nothing depends on `host`.

## Data

Each module owns a Postgres schema named after it — `identity`, `billing`, `quota`,
`notifications`, `appconfig`, `privacy`. Your tables live in `app`, so a platform upgrade
can never collide with them.

Migrations are per module (`db/migration/<module>/`), listed in `spring.flyway.locations`.
Hibernate runs with `ddl-auto: validate`: Flyway owns the schema, and two things editing the
same tables leaves no record of who did what.

There are no foreign keys across module schemas. Keeping schemas independent is worth more
than the constraint, and deletion goes through `UserDataContributor` rather than
`ON DELETE CASCADE`.

## Testing

- **Per module**: an `ApplicationContextRunner` test proving the configuration loads and
  exposes what it should. No database, so it runs in milliseconds.
- **Where behaviour exists**: plain unit tests — `AppVersion` ordering,
  `InMemoryRateLimiter` windows, JWT round-trips.
- **Once, in `host`**: `ArchitectureRulesTest` for the boundaries, and `ApplicationIT`
  (Testcontainers) that starts real Postgres, applies every migration, and checks the
  schema validates.

`ApplicationIT` skips when Docker is not running, so `./mvnw verify` stays green on a
machine without it. CI has Docker and runs it for real.

## Runtime settings

Some settings can be changed while the application runs, without a redeploy:

- `application.yml` supplies the value the app boots with.
- An override stored in `appconfig.app_setting` wins over it.
- `core` reads through the `RuntimeSettings` port and knows nothing about where the
  override came from. `appconfig` persists it. `admin` writes it. None of the three depends
  on the next.

Overrides are held in memory and refreshed on a short interval, because
`RateLimitFilter` reads on every request and a query there would tie the API's availability
to the settings table. A change applies at once on the instance that served the admin
request and within one refresh interval elsewhere.

Only keys declared in a `SettingCatalog` can be written, and values are validated against
the declared type. Without that, the table fills with typos — each one a setting somebody
believes they changed.

## Environments

Two: `dev` and `prod`, selected with `SPRING_PROFILES_ACTIVE`. Each is a separate
deployment with its own database and its own Redis, so their settings are separated by
construction rather than by a column.

`dev` is not "less strict" — every security check still applies, including the minimum
lengths for the JWT secret and the admin key. What differs is visibility: readable SQL,
Swagger UI, looser rate limits, faster settings refresh, the admin API on.

`prod` tightens the pool, turns off API docs, narrows actuator, disables `flyway clean`,
and leaves the admin API off unless you deliberately enable it.

## What this template does not decide for you

- Which analytics vendor, push provider or e-mail service. Ports plus no-ops; pick later.
- What your app sells. Entitlement ids and quota keys are strings, configured per app.
- Your limits. `quota` ships no defaults, and denies until you supply a `QuotaPolicy`.
- Roles and permissions. There is one kind of user.
