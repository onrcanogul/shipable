package dev.onrcanogul.appbackend.identity.api.dto;

import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sent when a user who has been using the app anonymously signs in for the first time.
 *
 * <p>The anonymous user is taken from the current session, not from the body: letting the
 * client name the account to absorb would let anyone absorb someone else's.
 */
public record LinkAnonymousAccountRequest(
        @NotNull AuthProvider provider,
        @NotBlank String identityToken) {
}
