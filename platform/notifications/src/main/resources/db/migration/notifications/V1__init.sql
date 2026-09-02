-- notifications module: the device registry.
-- Schema prefix: notifications

CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.device_token (
    id             uuid         PRIMARY KEY,
    user_id        uuid         NOT NULL,
    device_id      varchar(255) NOT NULL,
    token          varchar(512) NOT NULL,
    platform       varchar(16)  NOT NULL,
    locale         varchar(16),
    -- Set when APNs or FCM reports the token is dead, so we stop paying to send to an
    -- uninstalled app.
    invalidated_at timestamptz,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL
);

-- Unique on (user_id, device_id), not on the token: tokens rotate, and keying on them
-- would leave a row behind on every rotation and send each push several times to one phone.
CREATE UNIQUE INDEX ux_device_token_user_device ON notifications.device_token (user_id, device_id);
CREATE INDEX ix_device_token_user_active
    ON notifications.device_token (user_id) WHERE invalidated_at IS NULL;
