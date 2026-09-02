package dev.onrcanogul.appbackend.cache.internal.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.cache.CacheProperties;
import dev.onrcanogul.appbackend.cache.api.port.CacheService;
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
 * <p><b>A cache miss and a cache failure are the same thing here.</b> Every read that
 * throws is logged and treated as a miss, and every write that throws is logged and
 * dropped. Redis going down should make the app slower, never broken — a cache that can
 * take production offline is worse than no cache.
 *
 * <p>Stored as JSON rather than Java serialization: readable with {@code redis-cli}, and
 * not a deserialization gadget waiting for someone who gets write access to Redis.
 */
public class RedisCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    public RedisCacheService(
            StringRedisTemplate redis, ObjectMapper objectMapper, CacheProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
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
        try {
            redis.opsForValue().set(redisKey(key), objectMapper.writeValueAsString(value), ttl);
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
        try {
            redis.delete(redisKey(key));
        } catch (RuntimeException e) {
            log.warn("Cache eviction failed for '{}'", key, e);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        try {
            // keys() scans the whole keyspace and blocks Redis while it does. Acceptable
            // for an admin action after a settings change; never call it per request.
            Set<String> matching = redis.keys(redisKey(prefix) + "*");
            if (matching != null && !matching.isEmpty()) {
                redis.delete(matching);
            }
        } catch (RuntimeException e) {
            log.warn("Cache eviction failed for prefix '{}'", prefix, e);
        }
    }

    private String redisKey(String key) {
        return properties.key("cache", key);
    }
}
