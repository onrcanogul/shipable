package dev.onrcanogul.appbackend.identity.api.port;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.model.VerifiedIdentity;

/**
 * Verifies an identity token issued by one provider.
 *
 * <p>One implementation per provider, selected by {@link #provider()}. Adding a provider
 * means adding an implementation and a bean, never editing a switch statement.
 *
 * <p><b>Contract for implementations:</b> verify the signature against the provider's
 * JWKS, and check issuer, audience and expiry. A token that merely parses is not verified,
 * and returning a {@link VerifiedIdentity} for one is an account takeover.
 */
public interface IdentityTokenVerifier {

    AuthProvider provider();

    /**
     * @return the verified identity, or an error when the token is rejected. Transport and
     *         configuration problems throw instead, because those are our fault, not the
     *         caller's
     */
    Result<VerifiedIdentity> verify(String rawToken);
}
