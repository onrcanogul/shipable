package dev.onrcanogul.appbackend.identity.api.model;

/**
 * What a provider token proved, once verified.
 *
 * @param subject       provider-side stable user id (the "sub" claim)
 * @param email         may be null - Apple only hands it over on the very first sign-in,
 *                      so never treat it as a key
 * @param emailVerified whether the provider vouched for the address
 */
public record VerifiedIdentity(String subject, String email, boolean emailVerified) {
}
