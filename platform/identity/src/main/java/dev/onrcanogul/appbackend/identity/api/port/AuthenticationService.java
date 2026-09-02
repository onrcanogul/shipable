package dev.onrcanogul.appbackend.identity.api.port;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticatedUser;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticationResult;
import java.util.Optional;

/** The sign-in entry point. */
public interface AuthenticationService {

    /** Verifies a provider token and returns a session, creating the user on first use. */
    Result<AuthenticationResult> signInWithProvider(AuthProvider provider, String rawToken);

    /**
     * Creates or resumes a device-scoped anonymous session.
     *
     * <p>Must be find-or-create on the device id, or a client that retries ends up with a
     * second anonymous account and loses its history.
     */
    AuthenticationResult signInAnonymously(String deviceId);

    /** Exchanges a refresh token for a fresh pair. */
    Result<AuthenticationResult> refresh(String refreshToken);

    /** Revokes one refresh token. Idempotent. */
    void signOut(String refreshToken);

    /** Revokes every refresh token for a user, e.g. after "sign out on all devices". */
    void signOutEverywhere(UserId userId);

    /**
     * Folds an anonymous account into a signed-in one.
     *
     * <p>The user has been using the app anonymously and now signs in. Their data must
     * follow them, or signing in looks like data loss.
     */
    Result<AuthenticationResult> linkAnonymousAccount(UserId anonymousUserId, AuthProvider provider, String rawToken);

    Optional<AuthenticatedUser> findById(UserId userId);
}
