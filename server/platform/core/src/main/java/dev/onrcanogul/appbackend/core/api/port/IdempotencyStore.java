package dev.onrcanogul.appbackend.core.api.port;

import java.time.Duration;

/**
 * Remembers which {@code Idempotency-Key} values have already been used.
 *
 * <p>Mobile clients retry: a request that timed out on a train may well have succeeded on
 * the server. Without this, the retry charges twice, sends twice, creates twice.
 */
public interface IdempotencyStore {

    /**
     * Claims a key for the first time.
     *
     * @return true when the caller now owns the key and should do the work; false when
     *         someone already claimed it
     */
    boolean claim(String key, Duration ttl);

    /**
     * Releases a claim so the client can retry.
     *
     * <p>Called when the work failed in a way that leaves nothing behind — otherwise a
     * transient failure would lock the client out until the TTL expires.
     */
    void release(String key);
}
