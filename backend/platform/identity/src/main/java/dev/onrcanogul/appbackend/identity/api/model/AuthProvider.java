package dev.onrcanogul.appbackend.identity.api.model;

/**
 * Where a user's identity came from.
 *
 * <p>{@link #ANONYMOUS_DEVICE} exists so the app is usable before anyone signs in. Making
 * people create an account before they have seen the value is the single most reliable way
 * to lose them; that user is later folded into a real account by
 * {@code AuthenticationService.linkAnonymousAccount}.
 */
public enum AuthProvider {
    APPLE,
    GOOGLE,
    ANONYMOUS_DEVICE
}
