package dev.onrcanogul.appbackend.identity.internal.client;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.model.VerifiedIdentity;
import dev.onrcanogul.appbackend.identity.api.port.IdentityTokenVerifier;

/**
 * Google Sign-In token verification.
 *
 * <p>TODO:
 * <ol>
 *   <li>Fetch and cache the JWKS from
 *       {@code https://www.googleapis.com/oauth2/v3/certs}, honouring its
 *       Cache-Control header.</li>
 *   <li>Verify the RS256 signature.</li>
 *   <li>Check {@code iss} is {@code accounts.google.com} or
 *       {@code https://accounts.google.com} - Google issues both - {@code aud} is the
 *       OAuth client id for this app, and {@code exp} is in the future.</li>
 * </ol>
 *
 * <p>Note that iOS, Android and web builds each have their own client id. Accept the set
 * you ship, not a single value.
 */
public class GoogleIdentityTokenVerifier implements IdentityTokenVerifier {

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public Result<VerifiedIdentity> verify(String rawToken) {
        throw new UnsupportedOperationException("TODO: Google identity token verification is not implemented");
    }
}
