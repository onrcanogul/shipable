package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.FeatureFlagEntity;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Feature flags read from the database and held in memory.
 *
 * <p>Same reasoning as the other snapshots here: flags get checked on hot paths, and a
 * query per check would be a needless cost on every request that consults one.
 *
 * <p>An unknown flag returns the caller's default rather than throwing. A typo in a flag
 * name should degrade to "feature off", not take down the endpoint that checked it.
 *
 * <p>The refresh interval is the trade: a flag exists so you can turn something off
 * <i>now</i>, so keep it short. Thirty seconds is usually the right order of magnitude.
 */
public class DatabaseFeatureFlags implements FeatureFlags {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFeatureFlags.class);

    private final FeatureFlagRepository repository;

    private volatile Map<String, Flag> snapshot = Map.of();

    public DatabaseFeatureFlags(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isEnabled(String flag, boolean defaultValue) {
        Flag known = snapshot.get(flag);
        return known == null ? defaultValue : known.enabled();
    }

    @Override
    public Map<String, Boolean> clientFacingFlags() {
        return snapshot.entrySet().stream()
                .filter(entry -> entry.getValue().exposedToClient())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().enabled()));
    }

    @Scheduled(fixedDelayString = "${app.config.refresh-interval:PT60S}", initialDelay = 0)
    public void reload() {
        try {
            snapshot = repository.findAll().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            FeatureFlagEntity::getFlagKey,
                            entity -> new Flag(entity.isEnabled(), entity.isExposedToClient()),
                            (first, second) -> first));
        } catch (RuntimeException e) {
            // Keep the previous snapshot: reverting every flag to its default because one
            // query failed could re-enable the thing you just turned off.
            log.warn("Could not refresh feature flags, keeping the previous snapshot", e);
        }
    }

    private record Flag(boolean enabled, boolean exposedToClient) {
    }
}
