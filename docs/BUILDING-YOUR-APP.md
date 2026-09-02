# Building your app

You cloned the template. Here is how to get from that to a running feature.

## 1. Rename it

Two find-and-replaces, once:

- `dev.onrcanogul.appbackend` → your package
- `app-backend-template` → your artifact id (root `pom.xml` and every module's `<parent>`)

Then in `infra/.env`, set `APP_JWT_ISSUER` to your app's name.

## 2. Fill in `infra/.env`

    cd infra && cp .env.example .env

`APP_JWT_SECRET` is the only one you cannot skip — the app refuses to start without a
32-byte key. Generate it with `openssl rand -base64 48`.

RevenueCat keys can wait until you have a paywall. The billing module will start; it just
will not find any entitlements.

## 3. Run it

    cd infra && docker compose up --build

Swagger UI at https://localhost/swagger-ui.html.

## 4. Write your first feature

Say your app stores notes.

**Migration** — `domain/src/main/resources/db/migration/domain/V2__note.sql`:

    CREATE TABLE app.note (
        id         uuid        PRIMARY KEY,
        user_id    uuid        NOT NULL,
        body       text        NOT NULL,
        created_at timestamptz NOT NULL,
        updated_at timestamptz NOT NULL
    );
    CREATE INDEX ix_note_user ON app.note (user_id);

**Entity** — `internal/persistence/entity/NoteEntity.java`, extending `BaseEntity` so
`id`, `created_at` and `updated_at` are filled for you.

**Repository** — `internal/persistence/repository/NoteRepository.java`, a plain
`JpaRepository`. Always scope queries by `userId`; there is no framework stopping you from
reading someone else's rows.

**Port** — `api/port/NoteService.java`. **Implementation** — `internal/service/`.

**Controller** — `internal/web/NoteController.java`:

    @PostMapping
    public NoteResponse create(@Valid @RequestBody CreateNoteRequest request) {
        UserId userId = CurrentUserHolder.require().userId();
        return NoteResponse.from(noteService.create(userId, request.body()));
    }

Take the user from `CurrentUserHolder`, never from the request body — a user id the client
sends is a claim, and trusting one is how people read each other's data.

**Register the beans** in `DomainModuleConfiguration`. Nothing is component-scanned.

**Add the migration location** — already there: `classpath:db/migration/domain`.

## 5. The two beans that are easy to forget

**A `QuotaPolicy`**, or every metered call is denied:

    @Bean
    QuotaPolicy quotaPolicy() {
        return entitlements -> entitlements.has(EntitlementId.of("pro"))
                ? List.of(QuotaLimit.of(QuotaKey.of("notes.create"), 1000, Duration.ofDays(1)))
                : List.of(QuotaLimit.of(QuotaKey.of("notes.create"), 20, Duration.ofDays(1)));
    }

**A `UserDataContributor` per table holding user data**, or account deletion silently stops
covering your app:

    @Bean
    UserDataContributor noteUserData(NoteRepository repository) {
        return new NoteUserDataContributor(repository);
    }

## 6. Guarding a paid feature

    billingService.requireEntitlement(userId, EntitlementId.of("pro"));

403 with `entitlement_required` if they have not paid.

For something metered:

    quotaService.checkOrThrow(userId, QuotaKey.of("ai.requests"), 1);
    var result = doTheExpensiveThing();
    quotaService.record(userId, QuotaKey.of("ai.requests"), 1);

Check before, record after, with what was actually spent.

## 7. Errors

Throw; the shape is handled.

    throw new NotFoundException("Note");        // 404, code "not_found"
    throw new ForbiddenException("Not yours");  // 403, code "forbidden"

For something app-specific, extend `AppException` with your own code. Codes are a contract:
a shipped client will be branching on them long after you change the server.

## 8. Before you ship

- [ ] Implement the Apple and Google verifiers — until then nobody can sign in. See the
      class javadoc; the steps and the traps are written out.
- [ ] Implement `DefaultAuthenticationService` — persistence and refresh rotation.
- [ ] Implement `RevenueCatBillingProvider` and `RevenueCatWebhookProcessor`.
- [ ] Implement `DefaultQuotaService` window accounting, if you meter anything.
- [ ] Implement `DefaultAccountDeletionService` — the stores require working deletion.
- [ ] Swap in a real `PushSender` if you send push.
- [ ] Move rate limiting and idempotency to Redis or the database **before** the second
      instance. Both are in-memory and single-instance today.
- [ ] Set up database backups. There are none — see `infra/README.md`.
- [ ] Set `APP_MIN_VERSION` and fill in `appconfig.platform_config`.

Each item is a TODO in the code, next to the notes on how to do it.

## 9. Removing a module

Say you never send push:

1. Drop the dependency from `host/pom.xml` and `domain/pom.xml`.
2. Drop `NotificationsModuleConfiguration` from `@Import` in `Application`.
3. Drop `classpath:db/migration/notifications` from `spring.flyway.locations`.
4. Delete `platform/notifications` and its line in `platform/pom.xml`.

Nothing else refers to it. That is what the boundaries are for.
