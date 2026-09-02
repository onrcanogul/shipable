# platform/billing

Decides what a user has paid for, and decides it from RevenueCat — never from the client.

## Responsibility

- Keep a local snapshot of each user's RevenueCat entitlements.
- Receive and authenticate RevenueCat webhooks.
- Answer "does this user hold entitlement X?" fast and offline.

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/billing/entitlements` | bearer | What the current user has paid for |
| `POST` | `/api/v1/billing/entitlements/refresh` | bearer | Re-read from RevenueCat (restore purchases) |
| `POST` | `/api/v1/billing/webhooks/revenuecat` | shared secret | Inbound subscription events |

## Tables it owns

| Schema | Table | Purpose |
| --- | --- | --- |
| `billing` | `entitlement_snapshot` | Our copy of what RevenueCat says each user holds. |
| `billing` | `processed_webhook_event` | Applied event ids, for idempotency. Keeps the raw payload for replay. |

Migrations: `src/main/resources/db/migration/billing/`

## Interfaces it exposes

- `BillingService` — `entitlementsOf`, `hasEntitlement`, `requireEntitlement`,
  `refreshFromProvider`.
- `BillingProvider` — the outbound seam. Replace the bean to replace RevenueCat.
- `WebhookAuthenticator` — how an inbound webhook is proved authentic.
- `EntitlementId`, `CustomerEntitlements`, `ActiveEntitlement`, `Store`.

Spring wiring: `BillingModuleConfiguration`.

## Decisions worth knowing

- **Entitlement ids are strings, not an enum.** They are configured in the RevenueCat
  dashboard and differ per app. Declare the ones your app uses as constants in `domain`.
- **Reads come from the snapshot.** A paywall check runs on every screen; it must not
  depend on RevenueCat being reachable, and RevenueCat rate-limits.
- **`UserId` is the RevenueCat `app_user_id`.** Which is why it must never change for an
  account. Set it on the client with `Purchases.logIn(userId)` right after sign-in.
- **The webhook answers 202 and works afterwards.** RevenueCat times out and retries; doing
  the work inline turns a slow query into duplicate deliveries.
- **A grace period counts as active.** A failed card on renewal is not a cancellation.
- **Webhook secret comparison is constant-time.** String equality leaks the secret one byte
  at a time to anyone willing to measure, on an endpoint they can call freely.

## Setup checklist

1. RevenueCat dashboard → Project settings → API keys → copy the **secret** v2 key into
   `APP_REVENUECAT_API_KEY`. Not the public SDK key your app ships with.
2. Integrations → Webhooks → URL `https://your-host/api/v1/billing/webhooks/revenuecat`,
   Authorization header a long random string → the same value in
   `APP_REVENUECAT_WEBHOOK_SECRET`.
3. Client: call `Purchases.logIn(userId)` after sign-in so RevenueCat and this backend
   agree on who the customer is.

## What it explicitly does NOT do

- **No RevenueCat calls yet.** `RevenueCatBillingProvider.fetchEntitlements` throws; the
  exact endpoint, mapping and error handling are documented in the class.
- **No webhook processing yet.** `RevenueCatWebhookProcessor` throws. The controller still
  authenticates the request and answers 202, so the endpoint is safe to register.
- **No snapshot persistence yet.** `entitlementsOf` returns "no entitlements" — the safe
  default while the reads are unimplemented.
- **No purchase or receipt verification of its own.** RevenueCat talks to the App Store and
  Play; that is the reason to use it.
- **No prices, products, paywall copy or offerings.** The client gets those from the
  RevenueCat SDK directly.
- **No quota enforcement.** This says *what they bought*; `platform/quota` says *how much
  they may use*.
