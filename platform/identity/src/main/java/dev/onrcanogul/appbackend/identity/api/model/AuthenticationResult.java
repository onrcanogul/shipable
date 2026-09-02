package dev.onrcanogul.appbackend.identity.api.model;

/** A user plus the session issued for them. */
public record AuthenticationResult(AuthenticatedUser user, TokenPair tokens) {
}
