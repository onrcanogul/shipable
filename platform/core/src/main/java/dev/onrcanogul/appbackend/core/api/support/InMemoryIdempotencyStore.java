package dev.onrcanogul.appbackend.core.api.support;

import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Idempotency keys held in this process's memory.
 *
 * <p>Same caveat as {@link InMemoryRateLimiter}: single instance only, and a restart
 * forgets every claim. For anything that moves money or sends a message, back this with
 * the database — a unique index on the key gives you the same guarantee across replicas —
 * before you run a second instance.
 *
 * <p>TODO: add a JDBC-backed implementation.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, Long> expiryByKey = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryIdempotencyStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean claim(String key, Duration ttl) {
        long now = clock.millis();
        AtomicBoolean claimed = new AtomicBoolean(false);
        // compute() runs atomically for a given key, so exactly one caller sees `claimed`
        // set to true even when several arrive at once.
        expiryByKey.compute(key, (ignored, existingExpiry) -> {
            if (existingExpiry == null || existingExpiry <= now) {
                claimed.set(true);
                return now + ttl.toMillis();
            }
            return existingExpiry;
        });
        return claimed.get();
    }

    @Override
    public void release(String key) {
        expiryByKey.remove(key);
    }

    /** Drops keys whose TTL has passed, so the map does not grow without bound. */
    public void evictExpired() {
        long now = clock.millis();
        expiryByKey.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
