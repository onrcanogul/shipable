-- Runtime setting overrides, editable from the admin API.
--
-- Only overrides are stored. A setting nobody has changed has no row here, so this table
-- answers "what has actually been touched in production" in one query, and the boot
-- defaults stay in application.yml where they are reviewable in git.

CREATE TABLE appconfig.app_setting (
    id            uuid          PRIMARY KEY,
    setting_key   varchar(190)  NOT NULL,
    setting_value varchar(2048) NOT NULL,
    -- Who changed it. An untraceable change to a production rate limit is the kind of
    -- thing nobody remembers making.
    updated_by    varchar(190),
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL
);

CREATE UNIQUE INDEX ux_app_setting_key ON appconfig.app_setting (setting_key);

COMMENT ON TABLE appconfig.app_setting IS
    'Overrides for settings declared in a SettingCatalog. An unknown key is rejected on write, so this table cannot fill with typos.';
