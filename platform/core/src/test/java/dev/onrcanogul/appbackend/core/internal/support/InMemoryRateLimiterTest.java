package dev.onrcanogul.appbackend.core.internal.support;

import static org.assertj.core.api.Assertions.assertThat;

import dev.onrcanogul.appbackend.core.api.port.RateLimitPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);
    private final RateLimitPolicy policy = new RateLimitPolicy(3, Duration.ofMinutes(1));

    @Test
    @DisplayName("permits are granted up to the limit, then denied")
    void deniesPastTheLimit() {
        assertThat(limiter.tryAcquire("ip", policy).allowed()).isTrue();
        assertThat(limiter.tryAcquire("ip", policy).allowed()).isTrue();
        assertThat(limiter.tryAcquire("ip", policy).allowed()).isTrue();

        var denied = limiter.tryAcquire("ip", policy);
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfter()).isPositive();
    }

    @Test
    @DisplayName("keys are counted independently")
    void keysAreIndependent() {
        limiter.tryAcquire("a", policy);
        limiter.tryAcquire("a", policy);
        limiter.tryAcquire("a", policy);

        assertThat(limiter.tryAcquire("b", policy).allowed()).isTrue();
    }

    @Test
    @DisplayName("the budget resets when the window rolls over")
    void windowResets() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("ip", policy);
        }
        assertThat(limiter.tryAcquire("ip", policy).allowed()).isFalse();

        clock.advance(Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("ip", policy).allowed()).isTrue();
    }

    @Test
    @DisplayName("expired windows are evicted so the map does not grow forever")
    void expiredWindowsAreEvicted() {
        limiter.tryAcquire("ip", policy);
        clock.advance(Duration.ofMinutes(2));
        limiter.evictExpired();

        assertThat(limiter.tryAcquire("ip", policy).remaining()).isEqualTo(2);
    }

    /** A clock the test can move, so window logic is testable without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
