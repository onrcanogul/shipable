# Several apps on one server

One VPS, several backends. This describes how the pieces are split, what must differ per
app, and the mistakes that are expensive.

Nothing here is specific to this template. The shared layer is ordinary infrastructure —
another backend, a Node service or a static site can sit behind the same Caddy and use the
same Postgres.

## The shape

    ~/shared/                 shared stack: Caddy (80/443), Postgres, Redis
       └── creates the `appnet` network

    ~/app-one/                a clone of the template
       └── infra/multi-app/app/   just the API, joins appnet
    ~/app-two/                another clone
    ~/whatever-else/          not from this template; also joins appnet

    Caddy → app-one-api:8080     by container name, over appnet
          → app-two-api:8080
          → whatever-else:3000

**Caddy has to be shared.** Only one process can bind 80 and 443, so a second stack with
its own Caddy simply fails to start. That single fact is what makes this layout necessary;
everything else is a choice.

## What must differ per app

| Variable | Why it matters |
| --- | --- |
| `APP_JWT_SECRET` | **The one that really matters.** Share it and a token minted for one app is accepted by every other app on the box — anyone with an account on the free app has an account on the paid one. |
| `APP_JWT_ISSUER` | Validated on every token, so it is a second line of defence against exactly that. |
| `APP_ADMIN_API_KEY` | Otherwise one app's admin key reconfigures another. |
| `APP_REVENUECAT_*` | Separate RevenueCat projects. |
| `APP_DB_URL` / `USERNAME` / `PASSWORD` | Its own database and user. |
| `APP_CACHE_KEY_PREFIX` | What keeps apps out of each other's cache and rate limit counters in the shared Redis. |
| `APP_NAME` | Compose project and the container name Caddy proxies to. |
| `APP_LOCAL_PORT` | Loopback port for SSH tunnelling. 8081, 8082, … |

Everything describing the shared infrastructure — `APP_REDIS_HOST`, the Postgres host —
is identical in every app's `.env`.

## Separate databases, not separate schemas

Two reasons, and the first is not negotiable: this template names its schemas `identity`,
`billing`, `quota`, `appconfig`, `privacy`. Two apps in one database would collide on all
of them.

The second is blast radius. `flyway clean` and a restore both operate on a database. With
one database per app, neither can reach a neighbour.

One shared Postgres *container* is fine, and is what `create-app-database.sh` sets up. It
is one thing to back up, one set of metrics to watch, and one connection limit to manage.
Give an app its own Postgres when its traffic earns it — by then, moving it is a
`pg_dump | psql`, not a migration.

## Memory limits are not optional

The JVM starts with `-XX:MaxRAMPercentage=75`. With no container limit, that percentage is
taken from the **host's** memory — so two apps on a 16 GB box would each plan to use 12 GB.
They do not find out about each other until the OOM killer arrives.

Set `mem_limit` on every service. With a limit, the JVM reads the cgroup and sizes its heap
correctly.

A 16 GB box running five apps:

| | Limit |
| --- | --- |
| Each app | `1g` |
| Postgres | `2g` |
| Redis | `512m` |
| Caddy | `128m` |

About 7.6 GB committed, and the rest stays free as page cache — which is what Postgres
actually wants. That is comfortable, not tight.

## Connections

`prod` defaults to a pool of 20 per app. Five apps is 100, which is exactly the Postgres
default `max_connections`, leaving nothing for `psql` or `pg_dump`.

The shared stack raises `max_connections` to 200 and each app's `.env` sets
`APP_DB_POOL_SIZE=10`.

## Bringing up the server

**1. Shared stack, first.**

    cd ~/shared && cp .env.example .env && $EDITOR .env
    docker compose up -d

**2. A database for the app.**

    ./create-app-database.sh app_one

It prints the generated password once. Put it in the app's `.env`.

**3. The app.**

    git clone <the app's repo> ~/app-one
    cd ~/app-one/infra/multi-app/app
    cp .env.example .env && $EDITOR .env
    docker compose up -d --build

**4. A site block in `~/shared/Caddyfile`**, then reload without dropping connections:

    docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile

Point the DNS record at the server **before** this step. Caddy validates over HTTP; if the
name does not resolve yet, Let's Encrypt fails and backs off.

## Adding another app later

Steps 2 to 4. The shared stack is untouched.

## Backups

    ~/shared/backup.sh /var/backups/postgres

Dumps every database, one gzipped file each, prunes anything older than `KEEP_DAYS`
(default 14). Put it on a timer:

    0 3 * * *  /home/you/shared/backup.sh /var/backups/postgres >> /var/log/pg-backup.log 2>&1

Then copy the output **off the machine**. A backup that only exists on the server it
protects is a second copy of the same disk.

## Reaching the admin API

Caddy 404s `/api/admin/*` for every site, so it is not reachable from the internet. Each
app publishes its port on loopback for exactly this:

    ssh -L 8081:localhost:8081 user@server
    curl -H "X-Admin-Key: $KEY" http://localhost:8081/api/admin/v1/settings

## What still bites at this scale

**Scheduled jobs that mutate shared state.** Every instance runs its own `@Scheduled`
methods. Today that is fine — the three `reload()` jobs and the in-memory eviction are all
per-instance by design. But `processDueDeletions` and the quota retention job are still
TODO, and when they are implemented they will need a lock (ShedLock, or a Postgres advisory
lock) so several replicas of the *same* app do not run them at once. Several *different*
apps are not a problem: separate databases.

**One Caddy is one point of failure** for every site on the box. That is the trade for
automatic TLS and one place to configure. Reloads are graceful, so config changes are not
the risk; the machine is.
