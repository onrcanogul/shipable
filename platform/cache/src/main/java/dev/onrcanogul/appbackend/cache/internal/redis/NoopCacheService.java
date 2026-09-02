package dev.onrcanogul.appbackend.cache.internal.redis;

import dev.onrcanogul.appbackend.cache.api.port.CacheService;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The cache you get when Redis is turned off: always a miss.
 *
 * <p>So calling code can use {@link CacheService} unconditionally. A feature that only
 * compiles when Redis is enabled would push every caller into an if-statement, and one of
 * them would get it wrong.
 *
 * <p>Deliberately not an in-memory map. A per-instance cache silently disagrees with itself
 * across replicas, and that is a far harder bug than "the cache is off".
 */
public class NoopCacheService implements CacheService {

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        // nothing to store
    }

    @Override
    public <T> T getOrCompute(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        return loader.get();
    }

    @Override
    public void evict(String key) {
        // nothing to evict
    }

    @Override
    public void evictByPrefix(String prefix) {
        // nothing to evict
    }
}
