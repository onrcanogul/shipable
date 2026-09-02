package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;

/**
 * Feature flags read from the database.
 *
 * <p>An unknown flag returns the caller's default rather than throwing. A typo in a flag
 * name should degrade to "feature off", not take down the endpoint that checked it.
 *
 * <p>TODO: cache with a short TTL. This is checked on hot paths, and a database round trip
 * per check is a needless cost - but keep the TTL small, because the point of a flag is
 * being able to turn something off *now*.
 */
public class DatabaseFeatureFlags implements FeatureFlags {

    private final FeatureFlagRepository repository;

    public DatabaseFeatureFlags(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isEnabled(String flag, boolean defaultValue) {
        // TODO: repository.findByFlagKey(flag).map(FeatureFlagEntity::isEnabled)
        //       .orElse(defaultValue), behind a Caffeine cache.
        return defaultValue;
    }
}
