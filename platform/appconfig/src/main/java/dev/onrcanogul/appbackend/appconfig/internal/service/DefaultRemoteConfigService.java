package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.AppConfigProperties;
import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.model.PlatformConfig;
import dev.onrcanogul.appbackend.appconfig.api.model.RemoteConfig;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.PlatformConfigEntity;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.port.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.port.SettingKeys;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Version gating and maintenance state, from the database.
 *
 * <p><b>Held in memory.</b> {@code MinimumVersionFilter} asks on every request, so a query
 * per call would add one to every request in the application and tie the API's availability
 * to this table. There are at most a handful of rows; they are kept in a map and swapped
 * atomically.
 *
 * <p><b>Permissive on failure and when empty.</b> With no row, no override and no database,
 * every client is supported and nothing is in maintenance. This is the one place in the
 * platform that deliberately fails <i>open</i>: a version gate that fails closed takes the
 * entire app down the first time a query hiccups, which is the opposite of what a safety
 * valve is for.
 */
public class DefaultRemoteConfigService implements dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRemoteConfigService.class);

    private final PlatformConfigRepository repository;
    private final FeatureFlags featureFlags;
    private final RuntimeSettings settings;
    private final AppConfigProperties properties;

    private volatile Map<ClientPlatform, PlatformConfig> snapshot = new EnumMap<>(ClientPlatform.class);

    public DefaultRemoteConfigService(
            PlatformConfigRepository repository,
            FeatureFlags featureFlags,
            RuntimeSettings settings,
            AppConfigProperties properties) {
        this.repository = repository;
        this.featureFlags = featureFlags;
        this.settings = settings;
        this.properties = properties;
    }

    @Override
    public RemoteConfig configFor(ClientPlatform platform) {
        return new RemoteConfig(
                platform,
                platformConfig(platform),
                settings.getBoolean(SettingKeys.MAINTENANCE_MODE, false),
                emptyToNull(settings.getString(SettingKeys.MAINTENANCE_MESSAGE, "")),
                featureFlags.clientFacingFlags());
    }

    @Override
    public boolean isSupported(ClientPlatform platform, AppVersion version) {
        if (!settings.getBoolean(SettingKeys.VERSION_GATE_ENABLED, properties.versionGateEnabled())) {
            return true;
        }
        if (version == null) {
            // No version header: an older client, or one of our own tools. Locking those
            // out would be a self-inflicted outage.
            return true;
        }
        return !version.isOlderThan(platformConfig(platform).minimumSupportedVersion());
    }

    /**
     * Reloads the platform rows.
     *
     * <p>On failure the previous snapshot stays. Losing the gate for a refresh interval is
     * far better than rejecting every client because one query failed.
     */
    @Scheduled(fixedDelayString = "${app.config.refresh-interval:PT60S}", initialDelay = 0)
    public void reload() {
        try {
            Map<ClientPlatform, PlatformConfig> reloaded = new EnumMap<>(ClientPlatform.class);
            for (PlatformConfigEntity entity : repository.findAll()) {
                AppVersion minimum = AppVersion.parseOrNull(entity.getMinimumSupportedVersion());
                AppVersion latest = AppVersion.parseOrNull(entity.getLatestVersion());
                if (minimum == null) {
                    log.warn("platform_config row for {} has an unparseable minimum version '{}', ignoring it",
                            entity.getPlatform(), entity.getMinimumSupportedVersion());
                    continue;
                }
                reloaded.put(entity.getPlatform(),
                        new PlatformConfig(minimum, latest == null ? minimum : latest, entity.getUpdateUrl()));
            }
            snapshot = reloaded;
        } catch (RuntimeException e) {
            log.warn("Could not refresh platform config, keeping the previous snapshot", e);
        }
    }

    private PlatformConfig platformConfig(ClientPlatform platform) {
        return Optional.ofNullable(snapshot.get(platform)).orElseGet(this::permissiveFallback);
    }

    private PlatformConfig permissiveFallback() {
        AppVersion minimum = AppVersion.of(properties.defaultMinimumVersion());
        return new PlatformConfig(minimum, minimum, null);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
