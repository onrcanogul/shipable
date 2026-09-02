package dev.onrcanogul.appbackend.notifications.api.port;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.notifications.api.model.DeviceToken;
import dev.onrcanogul.appbackend.notifications.api.model.PushPlatform;
import java.util.List;

/**
 * The device registry.
 *
 * <p>Registration is upsert-by-device: push tokens rotate, and appending every rotation
 * would leave you sending each notification several times to the same phone.
 */
public interface DeviceTokenService {

    DeviceToken register(UserId userId, String deviceId, String token, PushPlatform platform, String locale);

    /** Called on sign-out, so the next person to use the phone does not get their pushes. */
    void unregister(UserId userId, String deviceId);

    List<DeviceToken> tokensOf(UserId userId);
}
