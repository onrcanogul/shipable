package dev.onrcanogul.appbackend.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Admin API settings, bound from {@code app.admin.*}.
 *
 * <p>This API can change rate limits, force every client to update, and put the app into
 * maintenance. It is the most dangerous surface in the application, so it is off unless you
 * turn it on and it refuses to start with a weak key.
 *
 * @param enabled     false by default. Leave it off in any environment where you do not
 *                    need it; an endpoint that does not exist cannot be attacked
 * @param apiKey      from APP_ADMIN_API_KEY. At least 32 characters — this single value is
 *                    the whole authentication story. Generate: openssl rand -base64 32
 * @param allowedIps  optional CIDR/exact allowlist. Empty means "any IP with the key",
 *                    which is a defensible position for a solo developer but a second lock
 *                    is cheap. Only meaningful behind a proxy that sets X-Forwarded-For
 */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(boolean enabled, String apiKey, @NotNull List<String> allowedIps) {

    public AdminProperties {
        allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
    }
}
