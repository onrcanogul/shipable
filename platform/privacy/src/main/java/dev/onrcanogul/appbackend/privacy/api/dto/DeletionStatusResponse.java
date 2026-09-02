package dev.onrcanogul.appbackend.privacy.api.dto;

import dev.onrcanogul.appbackend.privacy.api.model.DeletionRequest;
import java.time.Instant;

/**
 * @param cancellable true while the grace period is still running, so the app can show a
 *                    "cancel deletion" button that actually works
 */
public record DeletionStatusResponse(
        String status,
        Instant requestedAt,
        Instant scheduledFor,
        boolean cancellable) {

    public static DeletionStatusResponse from(DeletionRequest request) {
        return new DeletionStatusResponse(
                request.status().name(),
                request.requestedAt(),
                request.scheduledFor(),
                request.status() == dev.onrcanogul.appbackend.privacy.api.model.DeletionStatus.PENDING);
    }
}
