package dev.onrcanogul.appbackend.cache.internal.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.cache.CacheProperties;
import dev.onrcanogul.appbackend.cache.api.port.CacheService;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.settings.SettingKeys;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * JSON values in Redis.
 *
 * <p><b>A cache miss and a cache failure are the same thing here.</b> Every read that throws
 * is logged and treated as a miss, and every write that throws is logged and dropped. Redis
 * going down should make the app slower, never broken — a cache that can take production
 * offline is worse than no cache.
 *
 * <p>Stored as JSON rather than Java serialization: readable with {@code redis-cli}, and not
 * a deserialization gadget waiting for someone who gets write access to Redis.
 *
 * <p><b>Settings first, configuration as the default.</b> The bypass switch and the fallback
 * TTL are read from {@link RuntimeSettings} on every call, so both can be changed from the
 * admin API while the application runs; with nothing stored, the values from
 * {@code app.cache.*} apply. What cannot work that way is the connection itself and whether
 * these beans exist at all — those are decided once, at startup.
 */
public class RedisCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;
    private final RuntimeSettings settings;

    public RedisCacheService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            CacheProperties properties,
            RuntimeSettings settings) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.settings = settings;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        if (bypassed()) {
            return Optional.empty();
        }
        try {
            String raw = redis.opsForValue().get(redisKey(key));
            return raw == null ? Optional.empty() : Optional.of(objectMapper.readValue(raw, type));
        } catch (JsonProcessingException e) {
            // A stale entry whose shape no longer matches the class. Drop it rather than
            // failing forever after a refactor.
            log.warn("Cached value for '{}' no longer deserialises, evicting", key, e);
            evict(key);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Cache read failed for '{}', treating as a miss", key, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (bypassed()) {
            return;
        }
        try {
            redis.opsForValue().set(redisKey(key), objectMapper.writeValueAsString(value), effectiveTtl(ttl));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Cache write failed for '{}', continuing without caching", key, e);
        }
    }

    @Override
    public <T> T getOrCompute(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T computed = loader.get();
        if (computed != null) {
            put(key, computed, ttl);
        }
        return computed;
    }

    @Override
    public void evict(String key) {
        // Not gated by the bypass: eviction while bypassed still needs to clear whatever is
        // already in Redis, or turning the bypass off would serve stale values.
        try {
            redis.delete(redisKey(key));
        } catch (RuntimeException e) {
            log.warn("Cache eviction failed for '{}'", key, e);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        try {
            // keys() scans the whole keyspace and blocks Redis while it does. Acceptable for
            // an admin action after a settings change; never call it per request.
            Set<String> matching = redis.keys(redisKey(prefix) + "*");
            if (matching != null && !matching.isEmpty()) {
                redis.delete(matching);
            }
        } catch (RuntimeException e) {
            log.warn("Cache eviction failed for prefix '{}'", prefix, e);
        }
    }

    /** Runtime switch; falls back to "not bypassed" when nothing is stored. */
    private boolean bypassed() {
        return settings.getBoolean(SettingKeys.CACHE_BYPASS, false);
    }

    /**
     * A caller's explicit TTL always wins. Only when it is absent or nonsensical does the
     * setting apply, and only then the configured default.
     */
    private Duration effectiveTtl(Duration requested) {
        if (requested != null && !requested.isZero() && !requested.isNegative()) {
            return requested;
        }
        return settings.getDuration(SettingKeys.CACHE_DEFAULT_TTL, properties.defaultTtl());
    }

    private String redisKey(String key) {
        return properties.key("cache", key);
    }
}
