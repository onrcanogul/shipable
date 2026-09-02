# platform/quota

Says how much a user may spend, before they spend it.

## Responsibility

- Map entitlements onto limits, through the `QuotaPolicy` your app implements.
- Answer "may this go ahead?" before expensive work runs.
- Record what was actually consumed afterwards.

## Tables it owns

| Schema | Table | Purpose |
| --- | --- | --- |
| `quota` | `quota_usage` | Append-only consumption ledger, indexed for rolling-window sums. |

Migrations: `src/main/resources/db/migration/quota/`

## Interfaces it exposes

- `QuotaService` — `check`, `record`, `checkOrThrow`.
- `QuotaPolicy` — **the seam you implement**. Declare it as a bean in `domain`.
- `QuotaKey`, `QuotaLimit`, `QuotaDecision`, `QuotaExceededException`.

Spring wiring: `QuotaModuleConfiguration`.

## How you use it

```java
// in domain
@Bean
QuotaPolicy quotaPolicy() {
    return entitlements -> entitlements.has(EntitlementId.of("pro"))
            ? List.of(QuotaLimit.of(QuotaKey.of("ai.requests"), 500, Duration.ofDays(1)))
            : List.of(QuotaLimit.of(QuotaKey.of("ai.requests"), 10, Duration.ofDays(1)));
}
```

## Decisions worth knowing

- **Check before, record after.** Recording an estimate up front and never correcting it is
  how quota accounting drifts away from reality.
- **An unconfigured limit denies.** A missing limit is a configuration gap, not permission
  to spend freely — and when the spending is someone else's API bill, that distinction is
  the difference between a bug and an invoice.
- **Not the same as core's rate limiter.** That one stops strangers and is keyed by IP.
  This one enforces what a known user paid for.
- **Quota keys are strings.** A template cannot know what your app meters.

## What it explicitly does NOT do

- **No window arithmetic yet.** `check` throws once a limit exists; summing the rolling
  window is TODO.
- **No persistence yet.** `record` throws; `quota_usage` is created but never written.
- **No default limits.** `DenyByDefaultQuotaPolicy` returns an empty list on purpose.
- **No rate limiting.** Quota is budget over a window, not requests per second.
- **No billing.** It counts; it does not charge. Money is `billing`.
- **No retention.** `quota_usage` grows with every metered call until you schedule
  `deleteOlderThan`.
