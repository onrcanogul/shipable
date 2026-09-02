# domain

**Your app goes here.** Everything in `src/platform/` exists to serve this.

Ships empty on purpose — no example to delete, no half-written abstraction to work around.

## What you get for free

| Need | Where |
| --- | --- |
| Call the API | `request<T>('/api/v1/...')` from `../platform` |
| Errors, already parsed | `ApiError` with a stable `code` and the `requestId` |
| Who is signed in | `currentSession()`, `subscribeToSession()` |
| Has the user paid | `fetchEntitlements()`, `hasEntitlement()` |
| Feature switches | `isFeatureEnabled('...')` |
| Product events | `track('...')` |

Token refresh, the `Idempotency-Key` on mutations, the platform and version headers and the
force-update gate all happen inside `request` — you do not call them.

## Suggested layout

    domain/
      api/        types your screens share
      internal/   the calls and logic behind them
      screens/    UI

Only what you re-export needs to be public. The platform never imports from here.
