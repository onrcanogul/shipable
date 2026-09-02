package dev.onrcanogul.appbackend.identity.api.dto;

import dev.onrcanogul.appbackend.identity.api.model.AuthenticationResult;
import java.time.Instant;

/**
 * What the client gets back from every auth endpoint.
 *
 * <p>A DTO rather than the domain record so the wire format can stay stable while the
 * internal model moves.
 */
public record AuthenticationResponse(
        String userId,
        boolean anonymous,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt) {

    public static AuthenticationResponse from(AuthenticationResult result) {
        return new AuthenticationResponse(
                result.user().id().toString(),
                result.user().anonymous(),
                result.tokens().accessToken(),
                result.tokens().refreshToken(),
                result.tokens().accessTokenExpiresAt(),
                result.tokens().refreshTokenExpiresAt());
    }
}
