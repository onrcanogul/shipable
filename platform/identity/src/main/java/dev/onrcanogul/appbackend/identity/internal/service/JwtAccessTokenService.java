package dev.onrcanogul.appbackend.identity.internal.service;

import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.identity.IdentityProperties;
import dev.onrcanogul.appbackend.identity.api.port.AccessTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;

/**
 * Issues and validates the HMAC-signed access tokens this backend hands out.
 *
 * <p>Symmetric signing (HS256) because there is exactly one service here: it validates the
 * tokens it signed, so there is no third party to hand a public key to. If you ever split
 * the backend, move to RS256/ES256 — {@link AccessTokenService} exists precisely so that
 * change stays inside this class.
 *
 * <p>The secret comes from configuration, which reads it from the environment. It is never
 * in the repository, and a startup check refuses to run with a short one — a guessable
 * signing key means anyone can mint a token for any user.
 */
public class JwtAccessTokenService implements AccessTokenService {

    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey key;
    private final IdentityProperties properties;
    private final Clock clock;

    public JwtAccessTokenService(IdentityProperties properties, Clock clock) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.identity.jwt.secret must be at least " + MINIMUM_SECRET_BYTES
                            + " bytes. Generate one with: openssl rand -base64 48");
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret);
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String issue(UserId userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.jwt().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.jwt().accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    @Override
    public Result<UserId> validate(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.jwt().issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
            return Result.ok(UserId.of(claims.getSubject()));
        } catch (ExpiredJwtException e) {
            // Told apart from a forged token on purpose: the client's reaction differs.
            // Expired means "refresh"; invalid means "sign in again".
            return Result.err(ErrorCodes.TOKEN_EXPIRED, "Access token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            return Result.err(ErrorCodes.TOKEN_INVALID, "Access token is not valid");
        }
    }

    /**
     * Hashes a refresh token for storage.
     *
     * <p>Plain SHA-256 rather than bcrypt: refresh tokens are long random strings we
     * generated, not user-chosen passwords, so there is nothing to brute force and the
     * lookup has to be fast enough to run on every refresh.
     */
    public static String hashRefreshToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
