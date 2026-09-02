package dev.onrcanogul.appbackend.identity.api.model;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Instant;

/**
 * A user as the rest of the app sees them.
 *
 * <p>Carries no e-mail, no display name and no provider token on purpose: the rest of the
 * system needs to know <i>which</i> user is acting, not who they are. Keeping personal
 * data confined to this module is also what makes the deletion flow tractable.
 *
 * @param externalSubject provider-side subject; null for anonymous users
 */
public record AuthenticatedUser(
        UserId id,
        AuthProvider provider,
        String externalSubject,
        boolean anonymous,
        Instant createdAt) {
}
