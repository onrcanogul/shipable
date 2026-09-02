package dev.onrcanogul.appbackend.cache.internal;

import dev.onrcanogul.appbackend.cache.CacheProperties;
import dev.onrcanogul.appbackend.core.api.settings.SettingCatalog;
import dev.onrcanogul.appbackend.core.api.settings.SettingDefinition;
import dev.onrcanogul.appbackend.core.api.settings.SettingKeys;
import dev.onrcanogul.appbackend.core.api.settings.SettingType;
import java.util.List;

/**
 * The cache settings an operator can change at runtime.
 *
 * <p>Boot defaults come from the bound {@link CacheProperties}, so the admin API shows what
 * this deployment actually starts with rather than a value written down once and forgotten.
 *
 * <p>Deliberately short. Connection details and {@code app.cache.enabled} are absent because
 * they cannot take effect without a restart: the connection pool is built at startup, and
 * {@code enabled} decides which beans exist at all. Listing them here would offer a switch
 * that appears to work and does nothing — worse than not offering it.
 */
public class CacheSettingCatalog implements SettingCatalog {

    private final CacheProperties properties;

    public CacheSettingCatalog(CacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<SettingDefinition> definitions() {
        return List.of(
                SettingDefinition.of(
                        SettingKeys.CACHE_BYPASS,
                        SettingType.BOOLEAN,
                        "false",
                        "Makes every cache read a miss and every write a no-op, without dropping the "
                                + "Redis connection. For ruling the cache out as the cause of something, "
                                + "or shedding load from a struggling Redis."),
                SettingDefinition.of(
                        SettingKeys.CACHE_DEFAULT_TTL,
                        SettingType.DURATION,
                        properties.defaultTtl().toString(),
                        "TTL for cache writes that do not name one. A caller's explicit TTL always wins."));
    }
}
