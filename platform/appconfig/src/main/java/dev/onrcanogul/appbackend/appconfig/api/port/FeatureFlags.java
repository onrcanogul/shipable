package dev.onrcanogul.appbackend.appconfig.api.port;

import java.util.Map;

/**
 * Server-side feature switches.
 *
 * <p>The point is being able to turn something off without shipping a build and waiting on
 * review. That matters most for the feature that is costing you money or crashing.
 */
public interface FeatureFlags {

    /** @param defaultValue used when the flag is unknown, so a typo fails predictably */
    boolean isEnabled(String flag, boolean defaultValue);

    default boolean isEnabled(String flag) {
        return isEnabled(flag, false);
    }

    /**
     * The flags marked as client-facing, for {@code GET /api/v1/config}.
     *
     * <p>Only the ones explicitly marked. Some flags decide what the app draws, others what
     * the server does; sending the second kind announces what you are about to launch.
     */
    Map<String, Boolean> clientFacingFlags();
}
