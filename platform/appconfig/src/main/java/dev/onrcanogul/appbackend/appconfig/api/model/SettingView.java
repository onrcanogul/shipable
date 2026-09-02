package dev.onrcanogul.appbackend.appconfig.api.model;

import java.time.Instant;

/**
 * A setting as the admin API shows it: what it is, what it is set to, and where that value
 * came from.
 *
 * @param overridden true when a stored value is in effect. The distinction matters - "120
 *                   because nobody changed it" and "120 because someone set it to 120" look
 *                   identical without it, and only one of them survives a reset
 */
public record SettingView(
        String key,
        SettingType type,
        String effectiveValue,
        String bootDefault,
        boolean overridden,
        String description,
        Instant updatedAt,
        String updatedBy) {
}
