package dev.onrcanogul.appbackend.privacy.api.model;

/**
 * Where an account deletion request has got to.
 *
 * <p>{@link #PENDING} exists because deletion is scheduled rather than immediate. A grace
 * period turns "I tapped the wrong thing" into a cancellable mistake instead of a support
 * ticket about data that no longer exists.
 */
public enum DeletionStatus {
    /** Requested, inside the grace period, still cancellable. */
    PENDING,
    /** Grace period over, erasure running. */
    IN_PROGRESS,
    /** Every contributor has erased its data. */
    COMPLETED,
    /** Cancelled by the user during the grace period. */
    CANCELLED
}
