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
