package dev.onrcanogul.appbackend.notifications.api.model;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Instant;

/**
 * A registered device.
 *
 * @param deviceId the client's stable device identifier, so re-registering replaces the
 *                 previous token instead of accumulating dead ones
 * @param token    the APNs or FCM token, which rotates and must be re-registered
 */
public record DeviceToken(
        UserId userId,
        String deviceId,
        String token,
        PushPlatform platform,
        String locale,
        Instant registeredAt) {
}
