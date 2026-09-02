-- core module: no tables of its own yet.
--
-- core owns the request pipeline (errors, request context, rate limiting, idempotency)
-- and the shape of every other table via BaseEntity, but it stores nothing itself.
-- Rate limit counters and idempotency keys currently live in process memory; see
-- InMemoryRateLimiter and InMemoryIdempotencyStore for why, and what to do before you
-- run a second instance.
--
-- This file exists so the migration directory is present and versioned from V1, which
-- keeps the numbering honest when core does gain a table.
--
-- TODO: when idempotency moves to the database, add here:
--   CREATE TABLE core.idempotency_key (
--       id uuid PRIMARY KEY, scoped_key varchar(512) NOT NULL, expires_at timestamptz NOT NULL, ...
--   );
--   CREATE UNIQUE INDEX ux_idempotency_key ON core.idempotency_key (scoped_key);

CREATE SCHEMA IF NOT EXISTS core;
