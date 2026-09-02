-- domain module: your app's tables.
--
-- The platform owns schemas named after its modules (identity, billing, quota, ...).
-- Yours live here, in `app`, so a platform upgrade can never collide with your tables and
-- you can always tell at a glance which is which.
--
-- Migrations run in filename order across every module, so keep numbering from V1 here;
-- the module directories are separate locations and do not share a sequence.

CREATE SCHEMA IF NOT EXISTS app;

-- Example of the shape the platform expects. Delete it and write your own.
--
-- CREATE TABLE app.note (
--     id         uuid        PRIMARY KEY,
--     user_id    uuid        NOT NULL,
--     body       text        NOT NULL,
--     created_at timestamptz NOT NULL,
--     updated_at timestamptz NOT NULL
-- );
-- CREATE INDEX ix_note_user ON app.note (user_id);
--
-- Notes on that shape:
--   id / created_at / updated_at come from BaseEntity, which fills them on insert.
--   user_id is a plain uuid with no foreign key across schemas - keeping module schemas
--   independent is worth more than the constraint, and deletion is handled by
--   UserDataContributor rather than by ON DELETE CASCADE.
