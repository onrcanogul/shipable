package dev.onrcanogul.appbackend.notifications.internal.service;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.notifications.api.model.PushMessage;
import dev.onrcanogul.appbackend.notifications.api.port.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs instead of sending.
 *
 * <p>The default so the rest of the app can call {@code PushSender} from day one without
 * anyone having set up APNs certificates or a Firebase project. Swap in a real
 * implementation by defining your own {@link PushSender} bean; nothing that calls it
 * changes.
 *
 * <p>It logs at INFO rather than DEBUG on purpose: in development you want to see that the
 * push would have gone out.
 *
 * <p>TODO for a real implementation: FCM HTTP v1 for Android and APNs over HTTP/2 for iOS
 * (FCM can relay to APNs too, which is one integration instead of two). Send in batches,
 * and act on the "unregistered" responses by setting {@code invalidated_at} - otherwise
 * you keep paying to send to uninstalled apps.
 */
public class NoopPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(NoopPushSender.class);

    @Override
    public void sendToUser(UserId userId, PushMessage message) {
        log.info("[no-op push] to={} title='{}' body='{}'", userId, message.title(), message.body());
    }
}
