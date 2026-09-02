package dev.onrcanogul.appbackend.core.api.port;

import java.time.Duration;

/**
 * The answer to "may this caller proceed?".
 *
 * @param retryAfter how long until a permit frees up; {@link Duration#ZERO} when allowed
 */
public record RateLimitDecision(boolean allowed, int remaining, Duration retryAfter) {

    public static RateLimitDecision allow(int remaining) {
        return new RateLimitDecision(true, remaining, Duration.ZERO);
    }

    public static RateLimitDecision deny(Duration retryAfter) {
        return new RateLimitDecision(false, 0, retryAfter);
    }
}
