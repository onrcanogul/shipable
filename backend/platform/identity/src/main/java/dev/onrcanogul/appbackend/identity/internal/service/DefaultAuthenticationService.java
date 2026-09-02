package dev.onrcanogul.appbackend.identity.internal.service;

import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.identity.IdentityProperties;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticatedUser;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticationResult;
import dev.onrcanogul.appbackend.identity.api.model.TokenPair;
import dev.onrcanogul.appbackend.identity.api.port.AccessTokenService;
import dev.onrcanogul.appbackend.identity.api.port.AuthenticationService;
import dev.onrcanogul.appbackend.identity.api.port.IdentityTokenVerifier;
import dev.onrcanogul.appbackend.identity.internal.persistence.entity.RefreshTokenEntity;
import dev.onrcanogul.appbackend.identity.internal.persistence.entity.UserEntity;
import dev.onrcanogul.appbackend.identity.internal.persistence.repository.RefreshTokenRepository;
import dev.onrcanogul.appbackend.identity.internal.persistence.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sessions: creating them, refreshing them, ending them.
 *
 * <p>Anonymous sign-in, refresh and sign-out are implemented. Provider sign-in and linking
 * are not, because they depend on {@link IdentityTokenVerifier}, which still has to talk to
 * Apple's and Google's JWKS endpoints. The shape is here; only the verification is missing.
 */
public class DefaultAuthenticationService implements AuthenticationService {

    /**
     * 32 bytes of randomness, base64url encoded.
     *
     * <p>A refresh token is a bearer credential with a long life, so it has to be
     * unguessable. This is not a JWT on purpose: a JWT would be self-validating, and the
     * whole point of a refresh token is that the server can revoke it.
     */
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final Map<AuthProvider, IdentityTokenVerifier> verifiers = new EnumMap<>(AuthProvider.class);
    private final AccessTokenService accessTokenService;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final IdentityProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public DefaultAuthenticationService(
            List<IdentityTokenVerifier> verifiers,
            AccessTokenService accessTokenService,
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            IdentityProperties properties,
            Clock clock) {
        verifiers.forEach(verifier -> this.verifiers.put(verifier.provider(), verifier));
        this.accessTokenService = accessTokenService;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Result<AuthenticationResult> signInWithProvider(AuthProvider provider, String rawToken) {
        IdentityTokenVerifier verifier = requireVerifier(provider);
        // TODO: verifier.verify(rawToken), then find-or-create on (provider, subject) the
        // same way signInAnonymously does on deviceId, and issue a session.
        throw new UnsupportedOperationException(
                "TODO: " + provider + " sign-in needs " + verifier.getClass().getSimpleName()
                        + " to verify tokens first");
    }

    /**
     * Starts or resumes the anonymous session for a device.
     *
     * <p>Find-or-create, not create: a client that retries after a timeout must not end up
     * with a second account and lose the first one's history. The unique index on
     * {@code device_id} is what makes that safe when two requests race — one insert wins,
     * the other conflicts and re-reads.
     */
    @Override
    @Transactional
    public AuthenticationResult signInAnonymously(String deviceId) {
        UserEntity user = users.findByDeviceId(deviceId)
                .orElseGet(() -> insertAnonymous(deviceId));
        return issueSession(user);
    }

    private UserEntity insertAnonymous(String deviceId) {
        try {
            return users.save(UserEntity.anonymous(deviceId));
        } catch (DataIntegrityViolationException e) {
            // Another request created it between our read and this insert. Its row is the
            // one that exists now, so use it rather than failing a legitimate sign-in.
            return users.findByDeviceId(deviceId).orElseThrow(() -> e);
        }
    }

    /**
     * Exchanges a refresh token for a new pair.
     *
     * <p>The old token is revoked as part of the same transaction — rotation. If a revoked
     * token is ever presented again, either the client is retrying or someone stole it;
     * either way the answer is the same, and the session is refused rather than extended.
     */
    @Override
    @Transactional
    public Result<AuthenticationResult> refresh(String refreshToken) {
        Instant now = clock.instant();

        Optional<RefreshTokenEntity> stored =
                refreshTokens.findByTokenHash(JwtAccessTokenService.hashRefreshToken(refreshToken));

        if (stored.isEmpty() || !stored.get().isUsable(now)) {
            return Result.err(ErrorCodes.TOKEN_INVALID, "Refresh token is not valid");
        }

        RefreshTokenEntity current = stored.get();
        Optional<UserEntity> user = users.findById(current.getUserId());
        if (user.isEmpty() || user.get().getDeletedAt() != null) {
            return Result.err(ErrorCodes.TOKEN_INVALID, "Refresh token is not valid");
        }

        current.revoke(now);
        return Result.ok(issueSession(user.get()));
    }

    /** Idempotent: signing out twice, or with a token already revoked, is not an error. */
    @Override
    @Transactional
    public void signOut(String refreshToken) {
        refreshTokens.findByTokenHash(JwtAccessTokenService.hashRefreshToken(refreshToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    /**
     * Revokes every refresh token for a user.
     *
     * <p>Access tokens already issued stay valid until they expire. That is the trade for
     * not querying the database on every request, and the reason the access token TTL is
     * short.
     */
    @Override
    @Transactional
    public void signOutEverywhere(UserId userId) {
        refreshTokens.revokeAllForUser(userId.value(), clock.instant());
    }

    @Override
    public Result<AuthenticationResult> linkAnonymousAccount(
            UserId anonymousUserId, AuthProvider provider, String rawToken) {
        IdentityTokenVerifier verifier = requireVerifier(provider);
        // TODO: verify the token, then either promoteTo() this row when the provider
        // account is new - which keeps the user's id and everything keyed on it - or, when
        // it already exists as a separate user, move the data across and set merged_into.
        throw new UnsupportedOperationException(
                "TODO: linking needs " + verifier.getClass().getSimpleName() + " to verify tokens first");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> findById(UserId userId) {
        return users.findById(userId.value())
                .filter(user -> user.getDeletedAt() == null)
                .map(DefaultAuthenticationService::toModel);
    }

    /** Issues an access token and a fresh, stored refresh token. */
    private AuthenticationResult issueSession(UserEntity user) {
        Instant now = clock.instant();
        UserId userId = UserId.of(user.getId());

        String refreshToken = newRefreshToken();
        Instant refreshExpiresAt = now.plus(properties.jwt().refreshTokenTtl());
        refreshTokens.save(RefreshTokenEntity.issue(
                user.getId(), JwtAccessTokenService.hashRefreshToken(refreshToken), refreshExpiresAt));

        TokenPair tokens = new TokenPair(
                accessTokenService.issue(userId),
                refreshToken,
                now.plus(properties.jwt().accessTokenTtl()),
                refreshExpiresAt);

        return new AuthenticationResult(toModel(user), tokens);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static AuthenticatedUser toModel(UserEntity user) {
        return new AuthenticatedUser(
                UserId.of(user.getId()),
                user.getProvider(),
                user.getExternalSubject(),
                user.isAnonymous(),
                user.getCreatedAt());
    }

    private IdentityTokenVerifier requireVerifier(AuthProvider provider) {
        IdentityTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new IllegalStateException("No token verifier registered for provider " + provider);
        }
        return verifier;
    }
}
