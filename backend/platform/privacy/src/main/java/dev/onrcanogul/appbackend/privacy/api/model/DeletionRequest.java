package dev.onrcanogul.appbackend.privacy.api.model;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Instant;

/**
 * An account deletion request.
 *
 * @param scheduledFor when erasure will run; until then the user can cancel
 */
public record DeletionRequest(
        UserId userId,
        DeletionStatus status,
        Instant requestedAt,
        Instant scheduledFor,
        Instant completedAt) {
}
