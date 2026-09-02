package dev.onrcanogul.appbackend.appconfig.api.dto;

import dev.onrcanogul.appbackend.appconfig.api.model.RemoteConfig;
import java.util.Map;

/**
 * What the client gets from {@code GET /api/v1/config}.
 *
 * @param forceUpdate true when this client is below the minimum supported version, so the
 *                    app can show a blocking screen rather than discovering it later
 *                    through a 426 on some unrelated call
 */
public record RemoteConfigResponse(
        String minimumSupportedVersion,
        String latestVersion,
        String updateUrl,
        boolean forceUpdate,
        boolean maintenanceMode,
        String maintenanceMessage,
        Map<String, Boolean> featureFlags) {

    public static RemoteConfigResponse from(RemoteConfig config, boolean forceUpdate) {
        return new RemoteConfigResponse(
                String.valueOf(config.platformConfig().minimumSupportedVersion()),
                String.valueOf(config.platformConfig().latestVersion()),
                config.platformConfig().updateUrl(),
                forceUpdate,
                config.maintenanceMode(),
                config.maintenanceMessage(),
                config.featureFlags());
    }
}
