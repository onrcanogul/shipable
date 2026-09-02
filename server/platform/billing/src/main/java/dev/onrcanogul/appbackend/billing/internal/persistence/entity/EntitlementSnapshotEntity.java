package dev.onrcanogul.appbackend.billing.internal.persistence.entity;

import dev.onrcanogul.appbackend.billing.api.model.Store;
import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Our copy of one entitlement RevenueCat says a user holds.
 *
 * <p>A snapshot, not a source of truth: RevenueCat owns the answer, this table exists so
 * reads are fast and survive RevenueCat being down. When the two disagree, RevenueCat
 * wins and {@code refreshFromProvider} is how you settle it.
 */
@Entity
@Table(name = "entitlement_snapshot", schema = "billing")
public class EntitlementSnapshotEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entitlement_id", nullable = false, length = 128)
    private String entitlementId;

    @Column(name = "product_id", length = 255)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "store", nullable = false, length = 32)
    private Store store;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "will_renew", nullable = false)
    private boolean willRenew;

    @Column(name = "in_grace_period", nullable = false)
    private boolean inGracePeriod;

    /** When we last heard from RevenueCat about this row. */
    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected EntitlementSnapshotEntity() {
        // for JPA
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEntitlementId() {
        return entitlementId;
    }

    public String getProductId() {
        return productId;
    }

    public Store getStore() {
        return store;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isWillRenew() {
        return willRenew;
    }

    public boolean isInGracePeriod() {
        return inGracePeriod;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public boolean isActiveAt(Instant now) {
        return expiresAt == null || expiresAt.isAfter(now) || inGracePeriod;
    }
}
