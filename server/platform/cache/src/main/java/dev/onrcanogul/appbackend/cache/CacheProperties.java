package dev.onrcanogul.appbackend.cache;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Redis settings, bound from {@code app.cache.*}.
 *
 * @param enabled      false by default so a fresh clone runs with no Redis. Turning it on
 *                     moves rate limiting and idempotency off process memory, which is
 *                     what you need before running a second instance
 * @param keyPrefix    namespaces every key, so one Redis can serve dev and prod without
 *                     them reading each other's entries
 * @param defaultTtl   used by callers that do not name one
 */
@Validated
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(boolean enabled, String keyPrefix, @NotNull Duration defaultTtl) {

    public CacheProperties {
        keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "app" : keyPrefix.trim();
        defaultTtl = defaultTtl == null ? Duration.ofMinutes(10) : defaultTtl;
    }

    /** Fully qualified key, so nothing collides across environments or modules. */
    public String key(String namespace, String rest) {
        return keyPrefix + ":" + namespace + ":" + rest;
    }
}
