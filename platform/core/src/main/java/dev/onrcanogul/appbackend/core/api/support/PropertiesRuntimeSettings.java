package dev.onrcanogul.appbackend.core.api.support;

import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import java.util.Optional;

/**
 * The fallback: no overrides at all, so every caller gets its boot default.
 *
 * <p>Active when {@code appconfig} is not on the classpath, or before any override has been
 * stored. It exists so {@code core} works on its own — a module should not need the module
 * that happens to persist settings in order to read one.
 */
public class PropertiesRuntimeSettings implements RuntimeSettings {

    @Override
    public Optional<String> find(String key) {
        return Optional.empty();
    }
}
