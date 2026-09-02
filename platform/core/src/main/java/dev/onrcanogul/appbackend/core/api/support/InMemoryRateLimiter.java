package dev.onrcanogul.appbackend.core.api.support;

import dev.onrcanogul.appbackend.core.api.port.RateLimitDecision;
import dev.onrcanogul.appbackend.core.api.port.RateLimitPolicy;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter held in this process's memory.
 *
 * <p>Good enough for a single instance, which is where an indie app starts and often
 * stays. Two honest caveats:
 * <ul>
 *   <li><b>It does not survive a restart</b> and it is not shared between instances. The
 *       moment you run more than one replica, swap in a Redis-backed
 *       {@link RateLimiter}.</li>
 *   <li><b>Fixed windows allow a boundary burst</b> — up to twice the limit across two
 *       adjacent windows. Fine for abuse prevention, wrong for anything you bill on.</li>
 * </ul>
 *
 * <p>TODO: add a Redis implementation and select it by profile.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        long now = clock.millis();
        long windowMillis = policy.window().toMillis();
        long windowIndex = now / windowMillis;
        long windowEndsAt = (windowIndex + 1) * windowMillis;

        Window window = windows.compute(key, (ignored, existing) ->
                existing == null || existing.index != windowIndex
                        ? new Window(windowIndex, windowEndsAt)
                        : existing);

        int used = window.count.incrementAndGet();
        if (used > policy.permits()) {
            return RateLimitDecision.deny(Duration.ofMillis(Math.max(1, windowEndsAt - now)));
        }
        return RateLimitDecision.allow(policy.permits() - used);
    }

    /**
     * Drops windows that have already rolled over.
     *
     * <p>Without this the map grows one entry per distinct key forever, which is a slow
     * memory leak keyed by whatever an attacker chooses to send. Scheduled by
     * {@code CoreModuleConfiguration}.
     */
    public void evictExpired() {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> entry.getValue().endsAt <= now);
    }

    private static final class Window {
        private final long index;
        private final long endsAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long index, long endsAt) {
            this.index = index;
            this.endsAt = endsAt;
        }
    }
}
