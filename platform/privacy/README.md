# platform/privacy

Account deletion and data export — the parts of app-store compliance that are backend work.

Apple and Google both require an app that lets people create an account to let them delete
it **from inside the app**. "Email us to delete your account" is a review rejection.

## Responsibility

- Take deletion requests, hold them through a grace period, then erase.
- Assemble a data export from every module that stores user data.

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/account/deletion` | registered | Request deletion |
| `DELETE` | `/api/v1/account/deletion` | registered | Cancel during the grace period |
| `GET` | `/api/v1/account/deletion` | registered | Status of a request |
| `GET` | `/api/v1/account/export` | registered | Everything we hold about this user |

`registered` means a real account, not an anonymous device session — otherwise anyone
holding the phone could delete the account or download the export.

## Tables it owns

| Schema | Table | Purpose |
| --- | --- | --- |
| `privacy` | `deletion_request` | Status, schedule, and which contributors have already erased. |

Migrations: `src/main/resources/db/migration/privacy/`

## Interfaces it exposes

- `AccountDeletionService`, `DataExportService`.
- `DeletionRequest`, `DeletionStatus`, `DataExport`.

Spring wiring: `PrivacyModuleConfiguration`.

## How other modules take part

They implement `UserDataContributor` (defined in `core`) and publish it as a bean. Spring
injects every one of them here. **This is the point of the module:** a module added next
year joins deletion and export by publishing a bean, instead of by someone remembering to
update a checklist.

    @Bean
    UserDataContributor ordersUserData(OrderRepository repository) {
        return new OrdersUserDataContributor(repository);
    }

## Decisions worth knowing

- **Deletion is scheduled, not immediate.** A grace period turns "I tapped the wrong thing"
  into a cancellable mistake rather than a support ticket about data that no longer exists.
- **Erasure records which contributors finished.** It spans modules and can fail halfway; a
  retry must resume, not restart or give up.
- **Delete the identity row last.** It is the key everything else is found by; lose it first
  and the rest is orphaned rather than deleted.

## What it explicitly does NOT do

- **No deletion yet.** `DefaultAccountDeletionService` throws. The export fan-out *is*
  implemented; contributors currently return empty maps.
- **No scheduled sweep.** `processDueDeletions` is not wired to a job yet.
- **No legal-retention rules.** Purchase records usually must outlive the account.
  Anonymise those rather than deleting them, and write down which and why.
- **No provider-side deletion.** RevenueCat has its own deletion endpoint; deleting here
  does not delete there.
- **No export delivery.** The endpoint returns JSON inline. A large export should become a
  background job and a signed download link.
- **No consent or cookie tracking.** Not a full GDPR programme — the two obligations an
  indie app hits first.
