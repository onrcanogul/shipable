package dev.onrcanogul.appbackend.identity.internal.client;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.model.VerifiedIdentity;
import dev.onrcanogul.appbackend.identity.api.port.IdentityTokenVerifier;

/**
 * Sign in with Apple token verification.
 *
 * <p>TODO, in this order:
 * <ol>
 *   <li>Fetch the JWKS from {@code https://appleid.apple.com/auth/keys} and cache it,
 *       keyed by {@code kid}. Refetch on an unknown kid, not on every request - Apple
 *       rotates keys and rate-limits that endpoint.</li>
 *   <li>Verify the ES256 signature.</li>
 *   <li>Check {@code iss} is {@code https://appleid.apple.com}, {@code aud} is this app's
 *       bundle id, and {@code exp} is in the future.</li>
 *   <li>Check the nonce against the one the client sent, if you use one.</li>
 * </ol>
 *
 * <p>Two things worth knowing when you implement this: Apple sends {@code email} only on
 * the very first authorisation, so store it then or never; and the private-relay address
 * is the real one - do not try to resolve it.
 */
public class AppleIdentityTokenVerifier implements IdentityTokenVerifier {

    @Override
    public AuthProvider provider() {
        return AuthProvider.APPLE;
    }

    @Override
    public Result<VerifiedIdentity> verify(String rawToken) {
        // Throwing, not returning an empty identity: a verifier that accepts anything is
        // an account takeover, and a stub that silently succeeds is easy to ship by
        // accident.
        throw new UnsupportedOperationException("TODO: Apple identity token verification is not implemented");
    }
}
