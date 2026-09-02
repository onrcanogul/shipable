# platform/notifications

Knows which devices belong to which user, and how to reach them — with no provider wired
up yet.

## Responsibility

- Register and retire device push tokens.
- Expose `PushSender` and `EmailSender` so the rest of the app can send from day one.

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/devices` | bearer | Register/update this device's push token |
| `DELETE` | `/api/v1/devices/{deviceId}` | bearer | Stop sending push to this device |

## Tables it owns

| Schema | Table | Purpose |
| --- | --- | --- |
| `notifications` | `device_token` | One row per (user, device). `invalidated_at` marks dead tokens. |

Migrations: `src/main/resources/db/migration/notifications/`

## Interfaces it exposes

- `DeviceTokenService`, `PushSender`, `EmailSender`.
- `PushMessage`, `EmailMessage`, `DeviceToken`, `PushPlatform`.

Spring wiring: `NotificationsModuleConfiguration`. Also contributes a
`UserDataContributor`, so devices are erased on account deletion.

## Decisions worth knowing

- **Both senders are no-ops, guarded by `@ConditionalOnMissingBean`.** Define your own bean
  and it takes over with no change to calling code, so you can wire notifications through
  the app before choosing a provider.
- **Unique on `(user_id, device_id)`, not on the token.** Tokens rotate; keying on them
  leaves a row per rotation and sends each push several times to one phone.
- **Sending must never fail the caller's work.** A push that does not arrive is a missed
  notification; a checkout that rolls back because FCM was slow is a lost sale.

## What it explicitly does NOT do

- **No actual sending.** `NoopPushSender` and `NoopEmailSender` log. The APNs/FCM notes are
  in the class javadoc.
- **No persistence yet.** `DefaultDeviceTokenService` throws.
- **No marketing e-mail.** Transactional only — consent tracking and unsubscribe links
  belong in a tool built for them.
- **No scheduling, batching, or quiet hours.** Send now, or not at all.
- **No in-app notification inbox.** That is app behaviour; build it in `domain`.
