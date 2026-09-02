# infra

Everything needed to run this on one server.

## Local

    cd infra
    cp .env.example .env
    # fill in APP_JWT_SECRET at minimum; the app refuses to start without it
    docker compose up --build

Then:

- API — http://localhost:8080/api/v1/health
- Swagger UI — http://localhost:8080/swagger-ui.html

No Caddy locally and no TLS, which is the point: plain HTTP on loopback beats clicking
through a self-signed certificate warning every morning.

Add Redis with `--profile redis` (and `APP_CACHE_ENABLED=true` in `.env`).

## Production, one app on the machine

1. Point an A record at the server **before** starting Caddy. Let's Encrypt validates over
   HTTP; if the name does not resolve yet it fails and backs off.
2. `APP_DOMAIN=api.yourapp.com` and a real `ACME_EMAIL` in `.env`.
3. Open 80 and 443. Nothing else — Postgres publishes no port, and the API only binds
   loopback.
4. `docker compose --profile standalone up -d --build`

The `standalone` profile is what adds Caddy. Forgetting it is loud rather than subtle: the
API is only on loopback, so nothing answers on the domain at all.

TLS is issued and renewed by Caddy with no cron job and no certbot.

## Production, on a machine that hosts other things

Use [`attached/`](attached/) instead. It starts the API alone and expects the proxy, the
database and Redis to already exist on the machine — put there by whoever runs the server,
and shared with whatever else is hosted there.

Caddy cannot be duplicated (only one process can bind 80 and 443), so on such a machine the
edge belongs to the server, not to this app.

What the app needs from that environment — the network, the four things the proxy must do,
its own database — is in
[`../docs/RUNNING-ON-A-SHARED-SERVER.md`](../docs/RUNNING-ON-A-SHARED-SERVER.md). This
repository deliberately ships no server-level configuration: no site blocks for other
applications, no shared stack, no backup job. That is a different concern with a different
lifecycle, and vendoring it into every app clone is how it drifts.

## What is deliberately not exposed

- **Postgres publishes no port.** Reachable from the api container and nowhere else. An
  exposed 5432 with a weak password is how these servers get found.
- **The API publishes no port.** Caddy is the only way in, so TLS cannot be bypassed.
- **`/actuator/*` is 404 at the edge.** It is for your monitoring; it can report
  configuration and environment details.

## Files

| File | Purpose |
| --- | --- |
| `docker-compose.yml` | postgres + api; `--profile redis` adds Redis, `--profile standalone` adds Caddy |
| `attached/` | The API alone, for a machine whose proxy and database already exist |
| `Dockerfile` | Two-stage build; JRE runtime, non-root user, no source in the image |
| `Caddyfile` | TLS, proxying, security headers, actuator block |
| `.env.example` | Every variable, with notes on how to generate the secrets |

## Memory limits

Every service has one. This is not tuning: the JVM starts with
`-XX:MaxRAMPercentage=75`, and with no container limit that percentage comes from the
**host's** memory — so two containers on one box would each plan to use three quarters of
the machine. With a limit set, the JVM reads the cgroup and sizes its heap correctly.

Override per deployment with `APP_MEM_LIMIT`, `APP_POSTGRES_MEM_LIMIT` and friends.

## Backups

There are none. `postgres-data` is a Docker volume, which survives `docker compose down`
but not `down -v` and not a dead disk.

Before this holds anything you would miss:

    docker compose exec -T postgres pg_dump -U app appdb | gzip > backup-$(date +%F).sql.gz

Put that on a schedule and copy the output off the machine.
