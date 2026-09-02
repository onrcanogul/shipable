# infra

Everything needed to run this on one server.

## Local

    cd infra
    cp .env.example .env
    # fill in APP_JWT_SECRET at minimum; the app refuses to start without it
    docker compose up --build

Then:

- API — https://localhost/api/v1/health
- Swagger UI — https://localhost/swagger-ui.html

Caddy issues a local certificate for `localhost`, so your browser will warn once.

## Production, on a VPS

1. Point an A record at the server **before** starting Caddy. Let's Encrypt validates over
   HTTP; if the name does not resolve yet it fails and backs off.
2. `APP_DOMAIN=api.yourapp.com` and a real `ACME_EMAIL` in `.env`.
3. Open 80 and 443. Nothing else — Postgres and the API publish no ports.
4. `docker compose up -d --build`.

TLS is issued and renewed by Caddy with no cron job and no certbot.

## What is deliberately not exposed

- **Postgres publishes no port.** Reachable from the api container and nowhere else. An
  exposed 5432 with a weak password is how these servers get found.
- **The API publishes no port.** Caddy is the only way in, so TLS cannot be bypassed.
- **`/actuator/*` is 404 at the edge.** It is for your monitoring; it can report
  configuration and environment details.

## Files

| File | Purpose |
| --- | --- |
| `docker-compose.yml` | postgres + api + caddy |
| `Dockerfile` | Two-stage build; JRE runtime, non-root user, no source in the image |
| `Caddyfile` | TLS, proxying, security headers, actuator block |
| `.env.example` | Every variable, with notes on how to generate the secrets |

## Backups

There are none. `postgres-data` is a Docker volume, which survives `docker compose down`
but not `down -v` and not a dead disk.

Before this holds anything you would miss:

    docker compose exec -T postgres pg_dump -U app appdb | gzip > backup-$(date +%F).sql.gz

Put that on a schedule and copy the output off the machine.
