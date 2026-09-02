package dev.onrcanogul.appbackend.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.identity.IdentityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtAccessTokenServiceTest {

    private static final String SECRET = "a-test-signing-secret-of-at-least-32-bytes";

    private final Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    private final JwtAccessTokenService service = serviceAt(fixed);

    @Test
    @DisplayName("a token issued for a user validates back to that user")
    void roundTrips() {
        UserId userId = UserId.newId();

        String token = service.issue(userId);

        assertThat(service.validate(token).asOptional()).contains(userId);
    }

    @Test
    @DisplayName("an expired token is rejected as expired, not as invalid")
    void expiredTokenIsDistinguishable() {
        String token = service.issue(UserId.newId());

        // Same secret, clock moved past the 15 minute TTL.
        JwtAccessTokenService later = serviceAt(fixed.plus(Duration.ofMinutes(16)));

        assertThat(later.validate(token).failure())
                .get()
                .extracting(error -> error.code())
                .isEqualTo(ErrorCodes.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("a token signed with another key is rejected")
    void forgedTokenIsRejected() {
        JwtAccessTokenService attacker = new JwtAccessTokenService(
                propertiesWith("a-completely-different-secret-of-32-bytes+"), Clock.fixed(fixed, ZoneOffset.UTC));
        String forged = attacker.issue(UserId.newId());

        assertThat(service.validate(forged).failure())
                .get()
                .extracting(error -> error.code())
                .isEqualTo(ErrorCodes.TOKEN_INVALID);
    }

    @Test
    @DisplayName("refresh token hashing is stable and does not echo the input")
    void refreshTokenHashing() {
        String raw = "a-refresh-token";

        String hash = JwtAccessTokenService.hashRefreshToken(raw);

        assertThat(hash).hasSize(64).isEqualTo(JwtAccessTokenService.hashRefreshToken(raw));
        assertThat(hash).doesNotContain(raw);
    }

    private JwtAccessTokenService serviceAt(Instant instant) {
        return new JwtAccessTokenService(propertiesWith(SECRET), Clock.fixed(instant, ZoneOffset.UTC));
    }

    private IdentityProperties propertiesWith(String secret) {
        return new IdentityProperties(
                new IdentityProperties.Jwt(secret, "test-issuer", Duration.ofMinutes(15), Duration.ofDays(60)),
                new IdentityProperties.Apple(List.of()),
                new IdentityProperties.Google(List.of()));
    }
}
