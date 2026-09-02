package dev.onrcanogul.appbackend.identity.api.context;

import dev.onrcanogul.appbackend.core.api.model.UserId;

/**
 * The authenticated caller.
 *
 * @param anonymous true when the session came from a device sign-in rather than a
 *                  provider. Endpoints that must not be reachable anonymously check this
 *                  rather than assuming any session is a real account.
 */
public record CurrentUser(UserId userId, boolean anonymous) {
}
