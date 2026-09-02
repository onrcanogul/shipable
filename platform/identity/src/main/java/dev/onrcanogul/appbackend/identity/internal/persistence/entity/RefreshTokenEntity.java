package dev.onrcanogul.appbackend.identity.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One issued refresh token.
 *
 * <p>Only the hash is stored. A refresh token is a long-lived credential; keeping the
 * plaintext means a database leak hands out live sessions for every user at once.
 */
@Entity
@Table(name = "refresh_token", schema = "identity")
public class RefreshTokenEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** The device this session belongs to, so "sign out on this device" is possible. */
    @Column(name = "device_label", length = 128)
    private String deviceLabel;

    protected RefreshTokenEntity() {
        // for JPA
    }

    /**
     * @param tokenHash SHA-256 of the token we handed the client. The plaintext is never
     *                  stored, so a database leak cannot hand out live sessions
     */
    public static RefreshTokenEntity issue(UUID userId, String tokenHash, Instant expiresAt) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        return token;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
