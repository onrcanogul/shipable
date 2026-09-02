package dev.onrcanogul.appbackend.cache.api.port;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A small typed cache your app can use directly.
 *
 * <p>Spring's {@code @Cacheable} is also available and is the better choice for caching a
 * method's result. This exists for the cases annotations handle badly: a TTL that depends
 * on the value, caching something that is not a method call, or explicit invalidation from
 * a different class.
 *
 * <p><b>Every entry needs a TTL.</b> There is no un-expiring {@code put}, on purpose — a
 * cache without expiry is a memory leak that also serves stale data, and on a small VPS the
 * first symptom is Redis eating the box.
 */
public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    <T> void put(String key, T value, Duration ttl);

    /**
     * Returns the cached value, or computes and stores it.
     *
     * <p>Deliberately does <b>not</b> lock: two callers missing at once both compute, and
     * one wins. For an indie app that is the right trade — the alternative is distributed
     * locking, which fails in more interesting ways than a duplicated computation.
     */
    <T> T getOrCompute(String key, Class<T> type, Duration ttl, Supplier<T> loader);

    void evict(String key);

    /** Drops every key under a prefix. Use sparingly; it scans. */
    void evictByPrefix(String prefix);
}
