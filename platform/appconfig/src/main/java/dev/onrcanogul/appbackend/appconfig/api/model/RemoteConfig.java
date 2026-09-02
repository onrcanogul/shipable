package dev.onrcanogul.appbackend.appconfig.api.model;

import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import java.util.Map;

/**
 * Everything the client should know before it draws its first screen.
 *
 * <p>One request, answered before sign-in, so the app can show "please update" or "back
 * shortly" instead of failing in a way the user has to interpret.
 *
 * @param maintenanceMode when true, the client should show a maintenance screen rather
 *                        than retrying and filling your logs
 * @param featureFlags    remote on/off switches, so a feature can be disabled without an
 *                        App Store review cycle
 */
public record RemoteConfig(
        ClientPlatform platform,
        PlatformConfig platformConfig,
        boolean maintenanceMode,
        String maintenanceMessage,
        Map<String, Boolean> featureFlags) {

    public RemoteConfig {
        featureFlags = featureFlags == null ? Map.of() : Map.copyOf(featureFlags);
    }
}
