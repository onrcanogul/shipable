# platform/identity

Answers one question for the whole app: *who is making this request?*

## Responsibility

- Verify Sign in with Apple and Google Sign-In identity tokens.
- Create device-scoped anonymous sessions, so the app is usable before anyone signs up.
- Fold an anonymous account into a signed-in one without losing the user's data.
- Issue and validate **our own** access and refresh tokens.

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/social` | public | Sign in with an Apple/Google identity token |
| `POST` | `/api/v1/auth/anonymous` | public | Start or resume an anonymous device session |
| `POST` | `/api/v1/auth/refresh` | public | Exchange a refresh token for a new session |
| `POST` | `/api/v1/auth/link` | bearer | Attach a provider account to the current anonymous session |
| `POST` | `/api/v1/auth/signout` | public | Revoke one refresh token |
| `POST` | `/api/v1/auth/signout-everywhere` | bearer | Revoke every refresh token for the user |

## Tables it owns

| Schema | Table | Purpose |
| --- | --- | --- |
| `identity` | `app_user` | One row per user. Provider, subject, device id, `merged_into`, `deleted_at`. |
| `identity` | `refresh_token` | Issued sessions. Stores a SHA-256 hash, never the token. |

Migrations: `src/main/resources/db/migration/identity/`

## Interfaces it exposes

- `AuthenticationService` — sign in, refresh, sign out, link.
- `AccessTokenService` — issue/validate access tokens; the seam for changing signing
  strategy.
- `IdentityTokenVerifier` — one implementation per provider. Adding a provider is an
  implementation plus a `@Bean`, never a new branch in a switch.
- `CurrentUserHolder` / `CurrentUser` — how every other module asks who is calling.

Spring wiring: `IdentityModuleConfiguration`.

## Decisions worth knowing

- **We issue our own session tokens.** The provider token is verified once and discarded.
  Passing an Apple token around would make every endpoint depend on Apple being up.
- **Refresh tokens are stored hashed.** A database leak must not hand out live sessions.
- **Authentication is permissive, endpoints are strict.** The filter binds a user when a
  valid token is present and otherwise does nothing; endpoints call
  `CurrentUserHolder.require()`. A new endpoint is therefore closed by default rather than
  accidentally public because someone forgot a path pattern.
- **Anonymous is a first-class state.** `CurrentUserHolder.requireRegistered()` exists for
  endpoints that must not be reachable from a throwaway device session.

## What it explicitly does NOT do

- **No real token verification yet.** Both verifiers throw. JWKS fetching and caching,
  signature checks and issuer/audience/expiry validation are TODO — see the class javadoc
  for the exact steps and the traps. Until they land, guest sign-in is the only way in.
- **No provider sign-in yet.** `signInWithProvider` and `linkAnonymousAccount` throw,
  because both need the token verifiers above. Anonymous sign-in, refresh and sign-out are
  implemented and covered end to end by `ApplicationIT`.
- **No e-mail/password sign-in, no OTP.** Providers and anonymous devices only.
- **No roles or permissions.** There is one kind of user. If you need roles, they belong in
  your `domain` module or in a Spring Security upgrade.
- **No profile data.** No display names, avatars, or preferences. Keeping personal data
  confined here is what makes the deletion flow tractable.
- **No account deletion.** That is `platform/privacy`; this module only owns the
  `deleted_at` column it sets.
