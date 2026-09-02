package dev.onrcanogul.appbackend.notifications.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import dev.onrcanogul.appbackend.notifications.api.model.PushPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One registered device.
 *
 * <p>Unique on {@code (user_id, device_id)} rather than on the token: tokens rotate, and
 * keying on them would leave a row behind on every rotation and send each notification
 * several times to the same phone.
 */
@Entity
@Table(name = "device_token", schema = "notifications")
public class DeviceTokenEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private PushPlatform platform;

    @Column(name = "locale", length = 16)
    private String locale;

    /** Set when the push service tells us the token is dead, so we stop trying. */
    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    protected DeviceTokenEntity() {
        // for JPA
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getToken() {
        return token;
    }

    public PushPlatform getPlatform() {
        return platform;
    }

    public String getLocale() {
        return locale;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }
}
