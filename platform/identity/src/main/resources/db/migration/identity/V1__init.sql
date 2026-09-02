-- identity module: users and their sessions.
-- Schema prefix: identity

CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.app_user (
    id               uuid         PRIMARY KEY,
    provider         varchar(32)  NOT NULL,
    external_subject varchar(255),
    device_id        varchar(255),
    email            varchar(320),
    anonymous        boolean      NOT NULL DEFAULT false,
    merged_into      uuid         REFERENCES identity.app_user (id),
    deleted_at       timestamptz,
    created_at       timestamptz  NOT NULL,
    updated_at       timestamptz  NOT NULL
);

-- Partial unique indexes, not plain ones: anonymous users have no subject and signed-in
-- users have no device id, and NULLs would otherwise collide in a composite unique key.
-- These indexes are what make find-or-create safe when two taps race.
CREATE UNIQUE INDEX ux_app_user_provider_subject
    ON identity.app_user (provider, external_subject)
    WHERE external_subject IS NOT NULL;

CREATE UNIQUE INDEX ux_app_user_device
    ON identity.app_user (device_id)
    WHERE device_id IS NOT NULL;

CREATE TABLE identity.refresh_token (
    id           uuid         PRIMARY KEY,
    user_id      uuid         NOT NULL REFERENCES identity.app_user (id),
    -- SHA-256 hex. The plaintext is never stored: a database leak must not hand out
    -- live sessions.
    token_hash   varchar(128) NOT NULL,
    expires_at   timestamptz  NOT NULL,
    revoked_at   timestamptz,
    device_label varchar(128),
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL
);

CREATE UNIQUE INDEX ux_refresh_token_hash ON identity.refresh_token (token_hash);
CREATE INDEX ix_refresh_token_user ON identity.refresh_token (user_id) WHERE revoked_at IS NULL;
CREATE INDEX ix_refresh_token_expiry ON identity.refresh_token (expires_at);

COMMENT ON COLUMN identity.app_user.merged_into IS
    'Set when an anonymous account was folded into a signed-in one. The row is kept so old refresh tokens resolve.';
COMMENT ON COLUMN identity.app_user.deleted_at IS
    'Set by the privacy module when a deletion request is honoured.';
