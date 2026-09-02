# app-template

A starter for indie mobile apps: a Spring Boot backend and a React Native client, in one
repository, sharing one contract.

Everything an app needs before it does anything interesting is already built and
domain-agnostic — sign-in, subscriptions, quotas, remote config, push, account deletion, and
the unglamorous request-pipeline work underneath. Clone it, write your app in the two empty
`domain` modules, and never build this layer again.

    server/       Spring Boot, Java 21, PostgreSQL      → server/README.md
    mobile/       React Native, Expo SDK 57             → mobile/README.md

## Quick start

Backend first — the app needs something to talk to.

    cd server/infra
    cp .env.example .env
    # set APP_JWT_SECRET: openssl rand -base64 48
    docker compose up -d --build

Then the app:

    cd mobile
    cp .env.example .env      # point EXPO_PUBLIC_API_BASE_URL at the backend
    npm install
    npm start

Open it and you get a login screen. **Continue as guest** works end to end: the backend
issues a session, the app stores it, and the next call is authenticated.

## The two halves share a contract

They are in one repository so that contract changes in one place. The clearest example is
error handling: `ErrorCodes.java` and `mobile/src/platform/core/api/errors.ts` are a pair,
and the client branches on `code` — never on `message`.

The client mirrors the backend in *responsibility*, not file-for-file. It has no tables and
no SPI, so some server modules collapse into a couple of functions.

## Requirements

Java 21 and Docker for the backend; Node 22 for the app. Maven comes from the wrapper.

## Honest status

A **skeleton with real boundaries**, not a finished product. Structure, configuration, error
handling, the request pipeline, migrations, guest sign-in, the admin API and the deployment
setup are done. The third-party integrations are not:

| Works today | Still a TODO |
| --- | --- |
| Guest sign-in, refresh with rotation, sign-out | Apple/Google token verification |
| Request pipeline, errors, validation, rate limiting | Linking a guest account to a real one |
| Runtime settings and the admin API | RevenueCat API calls and webhook processing |
| Feature flags, version gating, maintenance mode | Quota window accounting |
| Redis cache, idempotency | Account deletion execution |
| Migrations, Docker, Caddy, CI/CD | Push and e-mail providers |

Every TODO sits in the code with notes on how to do it and which mistakes to avoid. The
per-half READMEs have the full checklists.

The principle behind the gaps: **an unimplemented feature denies rather than allows.** Nobody
is entitled to anything, no quota passes, and token verification throws rather than
returning a "verified" identity. A stub that silently succeeds is how a template becomes a
security incident.
