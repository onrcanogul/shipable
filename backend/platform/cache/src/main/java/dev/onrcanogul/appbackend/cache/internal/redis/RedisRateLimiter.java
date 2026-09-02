package dev.onrcanogul.appbackend.cache.internal.redis;

import dev.onrcanogul.appbackend.cache.CacheProperties;
import dev.onrcanogul.appbackend.core.api.port.RateLimitDecision;
import dev.onrcanogul.appbackend.core.api.port.RateLimitPolicy;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Fixed-window rate limiter shared across instances, in Redis.
 *
 * <p>The replacement for {@code InMemoryRateLimiter} once you run more than one replica:
 * with process memory, three instances behind a load balancer give a caller three times the
 * limit.
 *
 * <p><b>Why a Lua script.</b> Increment-then-expire is two round trips, and a crash between
 * them leaves a key with no TTL — a counter that never resets, permanently locking out one
 * IP. Redis runs a script atomically, so the increment and the expiry either both happen or
 * neither does. It also halves the network cost on a hot path.
 *
 * <p>Same fixed-window caveat as the in-memory version: up to twice the limit across a
 * window boundary. Fine for abuse prevention; do not bill on it.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    /**
     * Returns the count after incrementing. The TTL is only set on the first increment, so
     * a window is not extended by later requests inside it.
     */
    private static final RedisScript<Long> INCREMENT_AND_EXPIRE = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;
    private final CacheProperties properties;
    private final RateLimiter fallback;

    /**
     * @param fallback used when Redis is unreachable. Failing open would remove the limit
     *                 exactly when infrastructure is already struggling, and failing closed
     *                 would take the whole API down because a cache is down. Falling back to
     *                 a per-instance limiter keeps some protection either way
     */
    public RedisRateLimiter(StringRedisTemplate redis, CacheProperties properties, RateLimiter fallback) {
        this.redis = redis;
        this.properties = properties;
        this.fallback = fallback;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        long windowMillis = policy.window().toMillis();
        long windowIndex = System.currentTimeMillis() / windowMillis;
        String redisKey = properties.key("ratelimit", key + ":" + windowIndex);

        try {
            Long used = redis.execute(
                    INCREMENT_AND_EXPIRE, List.of(redisKey), String.valueOf(windowMillis));

            if (used == null) {
                return fallback.tryAcquire(key, policy);
            }
            if (used > policy.permits()) {
                long windowEndsAt = (windowIndex + 1) * windowMillis;
                return RateLimitDecision.deny(
                        Duration.ofMillis(Math.max(1, windowEndsAt - System.currentTimeMillis())));
            }
            return RateLimitDecision.allow(policy.permits() - used.intValue());

        } catch (RuntimeException e) {
            log.warn("Redis unavailable for rate limiting, falling back to in-process counting", e);
            return fallback.tryAcquire(key, policy);
        }
    }
}
