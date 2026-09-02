package dev.onrcanogul.appbackend.core.api.model;

import java.util.UUID;

/**
 * Identifies a user of this app.
 *
 * <p>Lives in {@code core} rather than in {@code identity} because almost every module
 * needs to name a user, and none of them should have to depend on the module that
 * authenticates one.
 *
 * <p>This is also the id handed to RevenueCat as {@code app_user_id}, so it must stay
 * stable for the life of the account.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId must not be null");
        }
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
