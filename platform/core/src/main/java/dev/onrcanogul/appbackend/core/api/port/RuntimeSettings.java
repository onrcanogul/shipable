package dev.onrcanogul.appbackend.core.api.port;

import java.time.Duration;
import java.util.Optional;

/**
 * Settings that can change while the application is running.
 *
 * <p>The layering: {@code application.yml} supplies the value the app boots with, and an
 * override stored at runtime wins over it. So a fresh clone works with no database rows at
 * all, and turning a knob in production does not need a redeploy.
 *
 * <p>Read-only on purpose. Writing goes through the admin module, which is behind its own
 * authentication — a module that reads a setting must not be able to change it.
 *
 * <p>Implementations are called on hot paths (the rate limit filter reads on every
 * request), so they must be cheap: cache, and do not hit the database per call.
 */
public interface RuntimeSettings {

    /** The raw override, if one is set. Empty means "use the boot default". */
    Optional<String> find(String key);

    default String getString(String key, String defaultValue) {
        return find(key).orElse(defaultValue);
    }

    default int getInt(String key, int defaultValue) {
        return find(key).map(value -> {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // A malformed override must not take the application down. Fall back to
                // the boot default; the admin API validates on write, so this only
                // happens if someone edited the table by hand.
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(value -> Boolean.parseBoolean(value.trim())).orElse(defaultValue);
    }

    default Duration getDuration(String key, Duration defaultValue) {
        return find(key).map(value -> {
            try {
                return DurationParser.parse(value.trim());
            } catch (RuntimeException e) {
                return defaultValue;
            }
        }).orElse(defaultValue);
    }
}
