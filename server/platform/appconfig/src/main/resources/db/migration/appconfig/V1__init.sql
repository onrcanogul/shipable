-- appconfig module: version gating, maintenance mode and feature flags.
-- Schema prefix: appconfig

CREATE SCHEMA IF NOT EXISTS appconfig;

-- In the database rather than in a config file, because raising the minimum version is
-- something you do in a hurry, at a bad moment, because a shipped build is doing damage.
-- Needing a redeploy for that is how the redeploy becomes the outage.
CREATE TABLE appconfig.platform_config (
    id                        uuid        PRIMARY KEY,
    platform                  varchar(16) NOT NULL,
    minimum_supported_version varchar(32) NOT NULL,
    latest_version            varchar(32) NOT NULL,
    update_url                varchar(512),
    maintenance_mode          boolean     NOT NULL DEFAULT false,
    maintenance_message       varchar(512),
    created_at                timestamptz NOT NULL,
    updated_at                timestamptz NOT NULL
);

CREATE UNIQUE INDEX ux_platform_config_platform ON appconfig.platform_config (platform);

CREATE TABLE appconfig.feature_flag (
    id                uuid         PRIMARY KEY,
    flag_key          varchar(128) NOT NULL,
    enabled           boolean      NOT NULL DEFAULT false,
    -- Some flags decide what the app draws; others decide what the server does. Sending
    -- the second kind to the client announces what you are about to launch.
    exposed_to_client boolean      NOT NULL DEFAULT false,
    description       varchar(512),
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL
);

CREATE UNIQUE INDEX ux_feature_flag_key ON appconfig.feature_flag (flag_key);

-- Seeded permissive: an empty table must not lock every client out of the app.
INSERT INTO appconfig.platform_config
    (id, platform, minimum_supported_version, latest_version, update_url, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'IOS', '0.0.0', '1.0.0', NULL, now(), now()),
    (gen_random_uuid(), 'ANDROID', '0.0.0', '1.0.0', NULL, now(), now());
