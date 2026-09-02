-- privacy module: account deletion requests.
-- Schema prefix: privacy

CREATE SCHEMA IF NOT EXISTS privacy;

CREATE TABLE privacy.deletion_request (
    id                  uuid        PRIMARY KEY,
    user_id             uuid        NOT NULL,
    status              varchar(16) NOT NULL,
    requested_at        timestamptz NOT NULL,
    -- Erasure runs after a grace period, so a mis-tap stays cancellable instead of
    -- becoming a support ticket about data that no longer exists.
    scheduled_for       timestamptz NOT NULL,
    completed_at        timestamptz,
    -- Which contributors have already erased. The fan-out spans modules and can fail
    -- halfway; without this a retry either starts over or gives up.
    completed_data_sets text,
    created_at          timestamptz NOT NULL,
    updated_at          timestamptz NOT NULL
);

-- One live request per user: tapping delete twice must not queue two erasures, and the
-- second tap must not reset the grace period.
CREATE UNIQUE INDEX ux_deletion_request_pending
    ON privacy.deletion_request (user_id)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

-- Shaped for the sweep job's only query.
CREATE INDEX ix_deletion_request_due
    ON privacy.deletion_request (scheduled_for)
    WHERE status = 'PENDING';
