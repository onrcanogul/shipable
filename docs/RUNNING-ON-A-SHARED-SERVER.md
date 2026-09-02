# Running on a shared server

This app can run on a machine it does not have to itself — one already hosting other
services, with a reverse proxy, a database and a Redis that someone else set up.

**This document is a list of requirements, not a setup guide.** How that machine is built,
what else runs on it, and how the proxy is configured are the server's business. This
repository is one application; it does not ship the server.

For a machine hosting only this app, use `infra/docker-compose.yml` instead — it brings its
own Postgres and its own Caddy behind `--profile standalone`.

## Running it

    cd infra/attached
    cp .env.example .env && $EDITOR .env
    docker compose up -d --build

That starts one container: the API. Nothing else.

## What it expects to find

### A Docker network

The container joins an existing network by name (`APP_SHARED_NETWORK`, default `appnet`).
Compose treats it as `external`, so it must already exist. This is what puts the app on the
same network as the proxy and the database.

### A reverse proxy in front of it

The proxy reaches the app at **`${APP_NAME}-api:8080`** over that network. The app publishes
no public port.

Four things the proxy must do:

| Requirement | Why |
| --- | --- |
| Terminate TLS | The app speaks plain HTTP |
| Set `X-Forwarded-For` | Rate limiting is keyed on the caller's address. The app trusts this header, so it is only safe because the app's port is not publicly reachable |
| Block `/api/admin/*` | It can change rate limits, force every client to update, and enable maintenance mode |
| Block `/actuator/*` | It reports configuration and environment details |

As a Caddy site block — the shape, not a file to copy into this repo:

    api.example.com {
        @blocked path /api/admin/* /actuator/*
        respond @blocked 404

        reverse_proxy my-app-api:8080 {
            header_up X-Forwarded-For {remote_host}
        }
    }

nginx, Traefik or anything else is equally fine; the four requirements are what matter.

### A Postgres database of its own

Not a schema in a shared database. This template creates schemas named `identity`,
`billing`, `quota`, `appconfig` and `privacy`, which would collide with another
application's. A restore or a `flyway clean` also operates on a whole database, and must
not be able to reach anything else.

One Postgres *server* shared by several applications is fine. What the app needs is a
database and a user of its own:

    CREATE USER my_app WITH PASSWORD '...';
    CREATE DATABASE my_app OWNER my_app;

The app creates its own schemas through Flyway on first start.

Keep an eye on `max_connections`. `APP_DB_POOL_SIZE` defaults to 10 here rather than the
single-app 20, because this Postgres serves more than this app.

### Redis, optionally

Only when `APP_CACHE_ENABLED=true`. It shares whatever Redis is on the machine and stays out
of other keys through `APP_CACHE_KEY_PREFIX`, which is namespaced into every key it writes.
If nothing else on the machine namespaces its keys, give this app its own Redis database
index instead.

## What must be unique to this app

| Variable | If it is not |
| --- | --- |
| `APP_JWT_SECRET` | Anything else signing tokens with the same secret can mint tokens this app accepts |
| `APP_JWT_ISSUER` | Tokens are checked against it, so it is the second line of defence against exactly that |
| `APP_ADMIN_API_KEY` | Another admin key on the machine could reconfigure this app |
| `APP_NAME` | Compose project and container name collide |
| `APP_LOCAL_PORT` | The loopback bind fails |
| `APP_CACHE_KEY_PREFIX` | It reads and writes another service's cache entries and rate limit counters |
| `APP_DB_URL` / user / password | Shared data, shared blast radius |

## Memory

Set `APP_MEM_LIMIT`. This matters more here than anywhere else: the JVM starts with
`-XX:MaxRAMPercentage=75`, and with no container limit that percentage comes from the
**host's** memory. On a shared machine the app would plan to use three quarters of it and
only find out about its neighbours when the OOM killer arrives.

`1g` is a reasonable starting point; the heap settles around 750 MB.

## Reaching the admin API

The proxy blocks it, so it is not reachable from the internet. The container publishes its
port on loopback for exactly this:

    ssh -L 8081:localhost:8081 user@server
    curl -H "X-Admin-Key: $KEY" http://localhost:8081/api/admin/v1/settings

## Backups

Not this app's job either. Whoever runs the machine backs up the Postgres server; this app's
database is one of the ones in it.

Worth telling them: everything in Redis is reconstructible and does not need backing up.

## What still needs attention as this grows

Scheduled jobs that mutate shared state. Every instance runs its own `@Scheduled` methods.
Today that is fine — the config refresh jobs and the in-memory eviction are all
per-instance by design. But `processDueDeletions` and the quota retention job are still
TODO, and once implemented they need a lock (ShedLock, or a Postgres advisory lock) so two
replicas of *this* app do not run them at once. Other applications on the machine are not a
concern: separate databases.
