-- billing module: our snapshot of RevenueCat, plus the webhook idempotency ledger.
-- Schema prefix: billing

CREATE SCHEMA IF NOT EXISTS billing;

-- A snapshot, not the source of truth. RevenueCat owns the answer; this table exists so a
-- paywall check is a local query and keeps working when RevenueCat does not.
CREATE TABLE billing.entitlement_snapshot (
    id              uuid         PRIMARY KEY,
    user_id         uuid         NOT NULL,
    entitlement_id  varchar(128) NOT NULL,
    product_id      varchar(255),
    store           varchar(32)  NOT NULL,
    expires_at      timestamptz,
    will_renew      boolean      NOT NULL DEFAULT false,
    in_grace_period boolean      NOT NULL DEFAULT false,
    synced_at       timestamptz  NOT NULL,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);

CREATE UNIQUE INDEX ux_entitlement_snapshot_user_entitlement
    ON billing.entitlement_snapshot (user_id, entitlement_id);
CREATE INDEX ix_entitlement_snapshot_expiry
    ON billing.entitlement_snapshot (expires_at) WHERE expires_at IS NOT NULL;

-- Idempotency ledger. RevenueCat delivers at-least-once and retries every non-2xx, so a
-- redelivered RENEWAL would otherwise be applied twice.
CREATE TABLE billing.processed_webhook_event (
    id          uuid         PRIMARY KEY,
    event_id    varchar(255) NOT NULL,
    event_type  varchar(64)  NOT NULL,
    app_user_id varchar(255),
    -- The raw payload is kept so an event that failed to process can be replayed after a
    -- fix rather than lost.
    payload     jsonb        NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

-- This index is what actually makes webhook handling idempotent: two deliveries racing
-- means one insert wins and the other conflicts.
CREATE UNIQUE INDEX ux_processed_webhook_event_id ON billing.processed_webhook_event (event_id);
CREATE INDEX ix_processed_webhook_event_user ON billing.processed_webhook_event (app_user_id);

COMMENT ON TABLE billing.processed_webhook_event IS
    'Applied RevenueCat events. TODO: add a retention job; this table grows with every renewal.';
