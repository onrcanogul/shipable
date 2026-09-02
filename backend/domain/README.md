# domain

**Your app goes here.** Every other module in this repository exists to serve this one.

The template ships it empty on purpose. There is no example to delete and no half-written
abstraction to work around.

## Package layout

Mirror the platform modules, so a reader who has seen one has seen them all:

    api/
      model/      value objects and enums other modules may use
      port/       interfaces you expose or need
      dto/        request/response payloads
    internal/
      service/    implementations
      persistence/entity/     JPA entities
      persistence/repository/ Spring Data repositories
      web/        controllers
      client/     outbound HTTP integrations

Only `api` is public. Nothing outside this module should import from `internal`, and an
ArchUnit test in `host` enforces that.

## What you get for free

| Need | Where it comes from |
| --- | --- |
| Who is calling | `CurrentUserHolder.require()` |
| Requires a real account | `CurrentUserHolder.requireRegistered()` |
| Has the user paid | `BillingService.requireEntitlement(userId, EntitlementId.of("pro"))` |
| Usage limits | `QuotaService.checkOrThrow(...)` then `record(...)` |
| Push / e-mail | `PushSender`, `EmailSender` |
| Product events | `AnalyticsRecorder` |
| Feature switches | `FeatureFlags.isEnabled("...")` |
| Errors, validation, request ids | `core` — throw `NotFoundException` and the shape is handled |

## Two beans to define early

**A `QuotaPolicy`**, or every metered call is denied. The platform ships no default limits
on purpose — see `platform/quota/README.md`.

**A `UserDataContributor` per table holding user data**, or account deletion quietly stops
covering your app. See `platform/privacy/README.md`.

## Tables

Yours live in the `app` schema, migrations in
`src/main/resources/db/migration/domain/`. Platform schemas are named after their modules,
so an upgrade can never collide with your tables.

## Where to start

[`../docs/BUILDING-YOUR-APP.md`](../docs/BUILDING-YOUR-APP.md) walks through adding a
feature end to end.
