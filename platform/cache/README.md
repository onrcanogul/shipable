# platform/cache

Redis, optional. Turn it on and rate limiting, idempotency and caching move off process
memory; leave it off and the in-memory defaults in `core` apply.

## Responsibility

- A `CacheService` your app can use directly.
- Redis-backed `RateLimiter` and `IdempotencyStore`, replacing core's in-memory versions.

## Turning it on

    app.cache.enabled=true          # APP_CACHE_ENABLED
    app.cache.key-prefix=myapp-prod # APP_CACHE_KEY_PREFIX

And start the container:

    docker compose --profile redis up

## Tables it owns

None.

## Interfaces it exposes

- `CacheService` — `get`, `put`, `getOrCompute`, `evict`, `evictByPrefix`.

It also publishes `@Primary` implementations of core's `RateLimiter` and
`IdempotencyStore`, so nothing that uses them needs to change.

Spring wiring: `CacheModuleConfiguration`.

## Decisions worth knowing

- **Off by default.** A fresh clone runs with no Redis container. This is the one thing to
  change before running a second instance: with in-memory rate limiting, three replicas
  behind a load balancer give a caller three times the limit.
- **Every entry needs a TTL.** There is no un-expiring `put`. A cache without expiry is a
  memory leak that also serves stale data, and on a small VPS the first symptom is Redis
  eating the box.
- **A cache failure is a cache miss.** Every Redis error is logged and treated as absent.
  Redis going down makes the app slower, never broken — a cache that can take production
  offline is worse than no cache.
- **The rate limiter is a Lua script.** Increment-then-expire is two round trips, and a
  crash between them leaves a counter with no TTL: one IP locked out permanently. A script
  is atomic and halves the network cost on a hot path.
- **Idempotency uses `SET NX PX`.** One atomic operation, so exactly one caller wins even
  across instances. Check-then-set would be a race, and the request that loses it is the
  duplicate charge you were preventing.
- **Redis failures fall back to per-instance memory**, not to failing open or closed.
  Failing open removes protection exactly when infrastructure is struggling; failing closed
  takes the API down because a cache is down.
- **Keys are namespaced** by `key-prefix`, so one Redis can serve dev and prod.
- **Values are JSON**, not Java serialization: readable with `redis-cli`, and not a
  deserialization gadget for anyone who gets write access to Redis.

## What it explicitly does NOT do

- **No Redis in the in-memory path.** With `enabled=false` there is no connection and no
  dependency at runtime.
- **No `@Cacheable` cache manager.** `CacheService` is explicit. Add a `RedisCacheManager`
  bean if you want the annotations too.
- **No locking in `getOrCompute`.** Two callers missing at once both compute. Distributed
  locking fails in more interesting ways than a duplicated computation.
- **No persistence.** Redis is configured with no AOF and no RDB: everything in it is
  reconstructible, and `allkeys-lru` makes it evict rather than refuse writes when full.
- **Not a session store.** Sessions are JWTs; refresh tokens live in Postgres.
