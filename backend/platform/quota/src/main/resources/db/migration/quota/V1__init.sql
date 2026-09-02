-- quota module: append-only consumption ledger.
-- Schema prefix: quota

CREATE SCHEMA IF NOT EXISTS quota;

-- Append-only on purpose: a running counter cannot answer "what did this user spend
-- yesterday" when they email support, and rolling windows need the timestamps anyway.
CREATE TABLE quota.quota_usage (
    id          uuid         PRIMARY KEY,
    user_id     uuid         NOT NULL,
    quota_key   varchar(128) NOT NULL,
    amount      bigint       NOT NULL,
    occurred_at timestamptz  NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

-- Shaped for the only hot query: sum(amount) for one user, one key, one window.
CREATE INDEX ix_quota_usage_window
    ON quota.quota_usage (user_id, quota_key, occurred_at DESC);

-- Supports the retention job below without scanning the table.
CREATE INDEX ix_quota_usage_occurred_at ON quota.quota_usage (occurred_at);

COMMENT ON TABLE quota.quota_usage IS
    'Consumption ledger. TODO: schedule deleteOlderThan; this table grows with every metered call.';
