package dev.onrcanogul.appbackend.identity.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent form of a user. Never leaves this module; the API speaks
 * {@code AuthenticatedUser}.
 */
@Entity
@Table(name = "app_user", schema = "identity")
public class UserEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private AuthProvider provider;

    @Column(name = "external_subject", length = 255)
    private String externalSubject;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "anonymous", nullable = false)
    private boolean anonymous;

    /** Set when this account was folded into another one; see the linking flow. */
    @Column(name = "merged_into")
    private UUID mergedInto;

    /** Set by the privacy module when a deletion request is honoured. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected UserEntity() {
        // for JPA
    }

    /**
     * A device-scoped user, created the first time an app is opened.
     *
     * <p>Anonymous until they sign in; {@link #promoteTo} then turns this same row into a
     * real account, which is what makes their data survive signing up.
     */
    public static UserEntity anonymous(String deviceId) {
        UserEntity user = new UserEntity();
        user.provider = AuthProvider.ANONYMOUS_DEVICE;
        user.deviceId = deviceId;
        user.anonymous = true;
        return user;
    }

    /** A user created directly from a provider sign-in, with no anonymous history. */
    public static UserEntity fromProvider(AuthProvider provider, String externalSubject, String email) {
        UserEntity user = new UserEntity();
        user.provider = provider;
        user.externalSubject = externalSubject;
        user.email = email;
        user.anonymous = false;
        return user;
    }

    /**
     * Turns an anonymous row into a signed-in one, in place.
     *
     * <p>In place on purpose: the id does not change, so everything keyed on it - purchases,
     * quota history, the user's own data - follows without being moved.
     */
    public void promoteTo(AuthProvider provider, String externalSubject, String email) {
        this.provider = provider;
        this.externalSubject = externalSubject;
        this.email = email;
        this.anonymous = false;
        this.deviceId = null;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getExternalSubject() {
        return externalSubject;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public UUID getMergedInto() {
        return mergedInto;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
