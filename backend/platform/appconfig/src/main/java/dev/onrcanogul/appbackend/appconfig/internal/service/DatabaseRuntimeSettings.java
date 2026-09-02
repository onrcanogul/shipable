package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.AppSettingEntity;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.AppSettingRepository;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Setting overrides, read from the database and held in memory.
 *
 * <p><b>Why a snapshot.</b> {@code RateLimitFilter} reads settings on every single request.
 * A database round trip there would add a query to every call in the application, and would
 * make the API's availability depend on the settings table. So the whole (small) table is
 * held in memory and swapped atomically.
 *
 * <p><b>Staleness.</b> A change made through the admin API applies immediately on the
 * instance that served the request and within one refresh interval everywhere else. For
 * settings like a rate limit or a maintenance flag, seconds of skew between replicas is
 * fine; nothing here needs to be transactional.
 *
 * <p><b>Failure.</b> If the refresh fails, the previous snapshot stays in place and callers
 * keep getting the last known values. A database blip must not silently reset every setting
 * to its boot default - which, for a rate limit someone tightened during an attack, would be
 * exactly the wrong moment.
 */
public class DatabaseRuntimeSettings implements RuntimeSettings {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRuntimeSettings.class);

    private final AppSettingRepository repository;

    /** Replaced wholesale on refresh, so a reader never sees a half-updated map. */
    private volatile Map<String, String> snapshot = Map.of();

    public DatabaseRuntimeSettings(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> find(String key) {
        return Optional.ofNullable(snapshot.get(key));
    }

    /**
     * Reloads from the database.
     *
     * <p>Called on a timer, and directly after a write so the instance that handled the
     * admin request reflects it at once.
     */
    @Scheduled(fixedDelayString = "${app.config.settings-refresh-interval:PT30S}", initialDelay = 0)
    public void reload() {
        try {
            snapshot = repository.findAll().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            AppSettingEntity::getSettingKey,
                            AppSettingEntity::getSettingValue,
                            // Two rows for one key should be impossible - there is a unique
                            // index - but losing every setting to an exception here would
                            // be a bad way to find that out.
                            (first, second) -> first));
        } catch (RuntimeException e) {
            log.warn("Could not refresh settings, keeping the previous snapshot", e);
        }
    }

    /** Everything currently overridden. Used by the admin API to build its listing. */
    public Map<String, String> overrides() {
        return snapshot;
    }

    /** Exposed for tests, which should not have to wait for a schedule. */
    void replaceSnapshot(Map<String, String> values) {
        snapshot = Map.copyOf(values);
    }
}
