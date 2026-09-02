package dev.onrcanogul.appbackend.notifications.internal.service;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.notifications.internal.persistence.repository.DeviceTokenRepository;
import java.util.Map;

/**
 * How this module takes part in account deletion and data export.
 *
 * <p>Every module that stores something about a user has one of these. It is what keeps
 * deletion correct as the app grows: a new module becomes visible to the deletion flow by
 * implementing this interface, rather than by someone remembering to update a checklist.
 */
public class NotificationsUserDataContributor implements UserDataContributor {

    private final DeviceTokenRepository repository;

    public NotificationsUserDataContributor(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public String dataSetName() {
        return "devices";
    }

    @Override
    public Map<String, Object> exportFor(UserId userId) {
        // TODO: export the registered devices, without the push tokens themselves - a
        // token is a credential for sending to that phone, not user data.
        return Map.of();
    }

    @Override
    public void eraseFor(UserId userId) {
        // Hard delete, not anonymise: a push token is only useful for sending, so keeping
        // one after deletion has no purpose and some risk.
        repository.deleteAllByUserId(userId.value());
    }
}
