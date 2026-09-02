package dev.onrcanogul.appbackend.core.api.port;

/**
 * Counts requests against a policy.
 *
 * <p>An interface rather than a concrete class because the right implementation depends on
 * how you deploy: one process can count in memory, several behind a load balancer cannot.
 * See {@code InMemoryRateLimiter} for the single-instance default.
 */
public interface RateLimiter {

    /**
     * Consumes one permit for {@code key} if the policy allows it.
     *
     * @param key    what is being limited — a user id, an IP, an endpoint, or a
     *               combination
     * @param policy the limit to apply
     */
    RateLimitDecision tryAcquire(String key, RateLimitPolicy policy);
}
