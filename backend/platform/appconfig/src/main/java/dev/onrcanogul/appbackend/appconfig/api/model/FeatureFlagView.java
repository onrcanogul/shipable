package dev.onrcanogul.appbackend.appconfig.api.model;

import java.time.Instant;

/**
 * A feature flag as the admin API shows it.
 *
 * @param exposedToClient whether {@code GET /api/v1/config} reports it. Flags that decide
 *                        server behaviour should stay false
 */
public record FeatureFlagView(
        String key,
        boolean enabled,
        boolean exposedToClient,
        String description,
        Instant updatedAt) {
}
