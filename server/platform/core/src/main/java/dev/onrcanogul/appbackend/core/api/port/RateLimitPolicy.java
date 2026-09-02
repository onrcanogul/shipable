package dev.onrcanogul.appbackend.core.api.port;

import java.time.Duration;

/**
 * How many requests are allowed in a window.
 *
 * @param permits how many are allowed
 * @param window  the window they are counted over
 */
public record RateLimitPolicy(int permits, Duration window) {

    public RateLimitPolicy {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
    }

    public static RateLimitPolicy perMinute(int permits) {
        return new RateLimitPolicy(permits, Duration.ofMinutes(1));
    }

    public static RateLimitPolicy perHour(int permits) {
        return new RateLimitPolicy(permits, Duration.ofHours(1));
    }
}
