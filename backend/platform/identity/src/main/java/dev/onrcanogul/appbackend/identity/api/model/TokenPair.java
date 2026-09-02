package dev.onrcanogul.appbackend.identity.api.model;

import java.time.Instant;

/**
 * The session we hand the client after a successful sign-in.
 *
 * <p>Short-lived access token, long-lived refresh token. The provider's token is verified
 * once and then discarded — passing an Apple or Google token around on every request would
 * mean every endpoint depends on that provider being reachable.
 *
 * @param refreshToken opaque, stored hashed server-side so a database leak does not hand
 *                     out sessions
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt) {
}
