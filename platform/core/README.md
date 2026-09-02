# platform/core

The request pipeline and the shapes everything else is built from. It stores nothing and
knows nothing about your app.

## Responsibility

- One error body for every failure, with a stable machine-readable `code`.
- `RequestContext` per request, pushed into the logging MDC so every log line is traceable.
- Bean Validation wired to that error body, with field-level detail.
- Coarse per-IP rate limiting, before authentication.
- `Idempotency-Key` handling, so a mobile retry does not create things twice.
- `BaseEntity`, `Result`, `UserId`, and the `UserDataContributor` SPI.

## Filter order

| Order | Filter | Why here |
| --- | --- | --- |
| `HIGHEST+10` | `RequestContextFilter` | Everything after it can log a request id |
| `HIGHEST+20` | `RateLimitFilter` | Reject floods before spending anything on them |
| `HIGHEST+30` | `IdempotencyFilter` | Catch retries before they reach a controller |
| `HIGHEST+35` | `MinimumVersionFilter` (appconfig) | Tell an old client to update, not that its token is bad |
| `HIGHEST+40` | `JwtAuthenticationFilter` (identity) | Authenticate last, once the request is worth the work |

## Tables it owns

None. `db/migration/core/V1__init.sql` creates the schema and documents why it is empty.

## Interfaces it exposes

- `AppError`, `ErrorCodes`, `ProblemBody`, and the `AppException` family.
- `Result<T>`, `UserId`.
- `RequestContext`, `RequestContextHolder`, `ClientPlatform`.
- `BaseEntity`.
- `RateLimiter`, `IdempotencyStore`, `UserDataContributor`.
- `ProblemResponseWriter` — so filters in other modules answer errors in the same shape.

Spring wiring: `CoreModuleConfiguration`.

## Decisions worth knowing

- **`UserId` lives here, not in `identity`.** Nearly every module needs to name a user;
  none of them should have to depend on the module that authenticates one.
- **`UserDataContributor` lives here, not in `privacy`.** Contributors would otherwise have
  to depend on the module that consumes them.
- **A `Clock` bean.** Anything time-dependent takes it, so tests can fix time instead of
  sleeping.
- **The catch-all handler says nothing specific.** Exception messages contain table names,
  file paths, and occasionally credentials.

## What it explicitly does NOT do

- **Rate limiting and idempotency are in-memory.** Single instance only, and a restart
  forgets everything. Both are behind interfaces; swap in Redis (or the database) before
  you run a second replica. The classes say so at the top.
- **Idempotent requests are rejected, not replayed.** A duplicate gets 409. Returning the
  original response means storing it — right for payments, overkill for creating a row.
- **The context does not cross threads.** `ThreadLocal`; handing work to `@Async` loses it.
  A `TaskDecorator` is a TODO.
- **No authentication or authorisation.** That is `identity`.
- **No caching, no metrics, no tracing.** Actuator is on in the host; anything beyond that
  is yours to add.
