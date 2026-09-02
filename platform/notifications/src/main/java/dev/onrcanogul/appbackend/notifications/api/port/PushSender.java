package dev.onrcanogul.appbackend.notifications.api.port;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.notifications.api.model.PushMessage;

/**
 * Sends a push to every device a user has registered.
 *
 * <p>Sending must never fail the caller's work. A push that does not arrive is a missed
 * notification; a checkout that rolls back because the push service was slow is a lost
 * sale. Implementations swallow and log their own failures.
 */
public interface PushSender {

    void sendToUser(UserId userId, PushMessage message);
}
