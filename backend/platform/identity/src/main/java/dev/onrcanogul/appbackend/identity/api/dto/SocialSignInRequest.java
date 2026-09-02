package dev.onrcanogul.appbackend.identity.api.dto;

import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param identityToken the raw token from Sign in with Apple or Google Sign-In
 */
public record SocialSignInRequest(
        @NotNull AuthProvider provider,
        @NotBlank String identityToken) {
}
