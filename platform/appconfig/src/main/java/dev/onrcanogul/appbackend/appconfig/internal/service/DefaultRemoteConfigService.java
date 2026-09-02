package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.AppConfigProperties;
import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.model.PlatformConfig;
import dev.onrcanogul.appbackend.appconfig.api.model.RemoteConfig;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import java.util.Map;

/**
 * Skeleton implementation of {@link RemoteConfigService}.
 *
 * <p>The fallback path is real and deliberately permissive: with no row in the database,
 * every client is supported and nothing is in maintenance. A config lookup that fails
 * closed would take the whole app down the first time someone dropped the table — the
 * opposite of what a safety valve is for.
 */
public class DefaultRemoteConfigService implements RemoteConfigService {

    private final PlatformConfigRepository repository;
    private final FeatureFlags featureFlags;
    private final AppConfigProperties properties;

    public DefaultRemoteConfigService(
            PlatformConfigRepository repository, FeatureFlags featureFlags, AppConfigProperties properties) {
        this.repository = repository;
        this.featureFlags = featureFlags;
        this.properties = properties;
    }

    @Override
    public RemoteConfig configFor(ClientPlatform platform) {
        // TODO: repository.findByPlatform(platform), map to RemoteConfig, and cache it -
        // this is called on every cold start, so it should not be a database round trip
        // per launch.
        return fallbackFor(platform);
    }

    @Override
    public boolean isSupported(ClientPlatform platform, AppVersion version) {
        if (version == null) {
            // No version header: an older client, or one of our own tools. Locking those
            // out would be a self-inflicted outage.
            return true;
        }
        return !version.isOlderThan(configFor(platform).platformConfig().minimumSupportedVersion());
    }

    /** Used until the table is populated, and whenever a platform has no row. */
    private RemoteConfig fallbackFor(ClientPlatform platform) {
        return new RemoteConfig(
                platform,
                new PlatformConfig(
                        AppVersion.of(properties.defaultMinimumVersion()),
                        AppVersion.of(properties.defaultMinimumVersion()),
                        null),
                false,
                null,
                Map.of());
    }

    /** Exposed so the controller can report both the config and the gate decision. */
    public FeatureFlags featureFlags() {
        return featureFlags;
    }
}
