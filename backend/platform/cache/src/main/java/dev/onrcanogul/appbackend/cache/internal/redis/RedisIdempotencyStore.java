package dev.onrcanogul.appbackend.cache.internal.redis;

import dev.onrcanogul.appbackend.cache.CacheProperties;
import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Idempotency keys in Redis, shared across instances.
 *
 * <p>Built on {@code SET key value NX PX ttl}, which is a single atomic operation: exactly
 * one caller gets {@code true} even when several arrive at the same millisecond on
 * different instances. Checking-then-setting would be a race, and the request that loses it
 * is the duplicate charge you were trying to prevent.
 */
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyStore.class);
    private static final String CLAIMED = "1";

    private final StringRedisTemplate redis;
    private final CacheProperties properties;
    private final IdempotencyStore fallback;

    public RedisIdempotencyStore(
            StringRedisTemplate redis, CacheProperties properties, IdempotencyStore fallback) {
        this.redis = redis;
        this.properties = properties;
        this.fallback = fallback;
    }

    @Override
    public boolean claim(String key, Duration ttl) {
        try {
            Boolean claimed = redis.opsForValue().setIfAbsent(redisKey(key), CLAIMED, ttl);
            return claimed != null ? claimed : fallback.claim(key, ttl);
        } catch (RuntimeException e) {
            // Falling back to per-instance memory rather than refusing the request: a
            // Redis outage should degrade duplicate protection, not stop people using the
            // app.
            log.warn("Redis unavailable for idempotency, falling back to in-process claims", e);
            return fallback.claim(key, ttl);
        }
    }

    @Override
    public void release(String key) {
        try {
            redis.delete(redisKey(key));
        } catch (RuntimeException e) {
            log.warn("Redis unavailable while releasing an idempotency key", e);
        }
        // Released in both stores: a claim may have been taken by the fallback.
        fallback.release(key);
    }

    private String redisKey(String key) {
        return properties.key("idempotency", key);
    }
}
