package dev.onrcanogul.appbackend.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for authentication, bound from {@code app.identity.*}.
 *
 * <p>Every secret here comes from an environment variable. Nothing in this record has a
 * default that would work in production, on purpose: an app that boots with a placeholder
 * signing key is worse than one that refuses to boot.
 */
@Validated
@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid Apple apple,
        @Valid Google google) {

    public IdentityProperties {
        // An app that only ships Sign in with Apple should not have to configure Google.
        // Absent provider config means an empty audience list, and a verifier with no
        // accepted audience rejects every token - the right way to be wrong.
        apple = apple == null ? new Apple(List.of()) : apple;
        google = google == null ? new Google(List.of()) : google;
    }

    /**
     * @param secret          HMAC signing key, at least 32 bytes. From APP_JWT_SECRET.
     *                        Generate with {@code openssl rand -base64 48}
     * @param issuer          the {@code iss} claim we set and require
     * @param accessTokenTtl  short by design - a leaked access token stays useful only
     *                        this long
     * @param refreshTokenTtl how long a user stays signed in without opening the app
     */
    public record Jwt(
            @NotBlank String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl) {
    }

    /**
     * @param bundleIds the {@code aud} values to accept. One per app target you ship
     */
    public record Apple(List<String> bundleIds) {

        public Apple {
            bundleIds = bundleIds == null ? List.of() : List.copyOf(bundleIds);
        }
    }

    /**
     * @param clientIds the {@code aud} values to accept. iOS, Android and web each have
     *                  their own, so this is a list rather than a single value
     */
    public record Google(List<String> clientIds) {

        public Google {
            clientIds = clientIds == null ? List.of() : List.copyOf(clientIds);
        }
    }
}
