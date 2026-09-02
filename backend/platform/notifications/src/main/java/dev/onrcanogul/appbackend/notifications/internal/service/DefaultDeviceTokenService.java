package dev.onrcanogul.appbackend.notifications.internal.service;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.notifications.api.model.DeviceToken;
import dev.onrcanogul.appbackend.notifications.api.model.PushPlatform;
import dev.onrcanogul.appbackend.notifications.api.port.DeviceTokenService;
import dev.onrcanogul.appbackend.notifications.internal.persistence.repository.DeviceTokenRepository;
import java.util.List;

/**
 * Skeleton implementation of {@link DeviceTokenService}.
 *
 * <p>TODO: upsert on {@code (user_id, device_id)}. Insert-only would accumulate a row per
 * token rotation and send every notification several times to the same phone.
 */
public class DefaultDeviceTokenService implements DeviceTokenService {

    private final DeviceTokenRepository repository;

    public DefaultDeviceTokenService(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeviceToken register(
            UserId userId, String deviceId, String token, PushPlatform platform, String locale) {
        throw new UnsupportedOperationException("TODO: device token registration is not implemented");
    }

    @Override
    public void unregister(UserId userId, String deviceId) {
        throw new UnsupportedOperationException("TODO: device token removal is not implemented");
    }

    @Override
    public List<DeviceToken> tokensOf(UserId userId) {
        throw new UnsupportedOperationException("TODO: device token lookup is not implemented");
    }
}
