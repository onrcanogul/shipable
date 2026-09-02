package dev.onrcanogul.appbackend.identity.api.context;

import dev.onrcanogul.appbackend.core.api.error.ForbiddenException;
import dev.onrcanogul.appbackend.core.api.error.UnauthorizedException;
import java.util.Optional;

/**
 * Carries the authenticated caller for the duration of a request.
 *
 * <p>Only {@code JwtAuthenticationFilter} writes here. Controllers read it rather than
 * taking a user id parameter — a user id that arrives in the request body is a claim from
 * the client, and trusting one is how people read each other's data.
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static Optional<CurrentUser> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** The caller, or 401 when there is none. */
    public static CurrentUser require() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw UnauthorizedException.missingCredentials();
        }
        return user;
    }

    /**
     * The caller, or 401/403 when there is none or they are anonymous.
     *
     * <p>For endpoints that need a real account: anything tied to a purchase, an export, or
     * data the user would expect to survive reinstalling the app.
     */
    public static CurrentUser requireRegistered() {
        CurrentUser user = require();
        if (user.anonymous()) {
            throw new ForbiddenException("This action requires a signed-in account");
        }
        return user;
    }

    /** Called by JwtAuthenticationFilter only. */
    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    /** Called by JwtAuthenticationFilter only, from a finally block. */
    public static void clear() {
        CURRENT.remove();
    }
}
