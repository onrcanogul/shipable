package dev.onrcanogul.appbackend.identity.internal.service;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticatedUser;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticationResult;
import dev.onrcanogul.appbackend.identity.api.port.AccessTokenService;
import dev.onrcanogul.appbackend.identity.api.port.AuthenticationService;
import dev.onrcanogul.appbackend.identity.api.port.IdentityTokenVerifier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skeleton implementation of {@link AuthenticationService}.
 *
 * <p>The wiring is decided; the persistence is not. Verifiers are selected from a map
 * keyed by provider, so adding a provider never edits this class.
 *
 * <p>Notes for whoever implements the TODOs, because these are the parts that go wrong:
 * <ul>
 *   <li><b>Sign-in must be find-or-create</b> on {@code (provider, subject)}, inside one
 *       transaction with a unique index behind it. Two taps on a slow network are two
 *       concurrent requests.</li>
 *   <li><b>Refresh should rotate.</b> Issue a new refresh token and revoke the old one on
 *       every refresh; if a revoked token is presented again, that is a replay — revoke
 *       the whole family.</li>
 *   <li><b>Linking is the risky one.</b> If the provider account already exists as a
 *       separate user, you have two accounts and must decide which survives. Decide it
 *       once, here, and write it down.</li>
 * </ul>
 */
public class DefaultAuthenticationService implements AuthenticationService {

    private final Map<AuthProvider, IdentityTokenVerifier> verifiers = new EnumMap<>(AuthProvider.class);
    private final AccessTokenService accessTokenService;

    public DefaultAuthenticationService(
            List<IdentityTokenVerifier> verifiers, AccessTokenService accessTokenService) {
        verifiers.forEach(verifier -> this.verifiers.put(verifier.provider(), verifier));
        this.accessTokenService = accessTokenService;
    }

    @Override
    public Result<AuthenticationResult> signInWithProvider(AuthProvider provider, String rawToken) {
        IdentityTokenVerifier verifier = requireVerifier(provider);
        // TODO: verifier.verify(rawToken) -> find-or-create UserEntity on
        // (provider, subject) -> issue an access token and persist a hashed refresh token.
        throw new UnsupportedOperationException("TODO: signInWithProvider is not implemented");
    }

    @Override
    public AuthenticationResult signInAnonymously(String deviceId) {
        // TODO: find-or-create UserEntity on deviceId, then issue a session. Find-or-create
        // rather than create, or a client retry silently strands the user's history on an
        // orphaned account.
        throw new UnsupportedOperationException("TODO: signInAnonymously is not implemented");
    }

    @Override
    public Result<AuthenticationResult> refresh(String refreshToken) {
        // TODO: look up JwtAccessTokenService.hashRefreshToken(refreshToken), check it is
        // usable, rotate it, and issue a new pair.
        throw new UnsupportedOperationException("TODO: refresh is not implemented");
    }

    @Override
    public void signOut(String refreshToken) {
        // TODO: mark the matching row revoked. Idempotent: signing out twice is not an
        // error, and neither is signing out with a token that was already revoked.
        throw new UnsupportedOperationException("TODO: signOut is not implemented");
    }

    @Override
    public void signOutEverywhere(UserId userId) {
        // TODO: RefreshTokenRepository.revokeAllForUser. Access tokens already issued stay
        // valid until they expire - that is the trade for not checking the database on
        // every request, and it is why the access token TTL is short.
        throw new UnsupportedOperationException("TODO: signOutEverywhere is not implemented");
    }

    @Override
    public Result<AuthenticationResult> linkAnonymousAccount(
            UserId anonymousUserId, AuthProvider provider, String rawToken) {
        IdentityTokenVerifier verifier = requireVerifier(provider);
        // TODO: verify the token, then in one transaction either promote the anonymous
        // user in place (when the provider account is new) or move their data onto the
        // existing account and set merged_into (when it is not).
        // Every module holding user data has to move with it - see UserDataContributor.
        throw new UnsupportedOperationException("TODO: linkAnonymousAccount is not implemented");
    }

    @Override
    public Optional<AuthenticatedUser> findById(UserId userId) {
        // TODO: UserRepository.findById(userId.value()).map(this::toModel)
        throw new UnsupportedOperationException("TODO: findById is not implemented");
    }

    private IdentityTokenVerifier requireVerifier(AuthProvider provider) {
        IdentityTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new IllegalStateException("No token verifier registered for provider " + provider);
        }
        return verifier;
    }
}
