package dev.onrcanogul.appbackend.quota.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One consumption record.
 *
 * <p>An append-only ledger rather than a running counter. A counter cannot answer "what did
 * this user spend yesterday" when they email support, and rolling windows need the
 * individual timestamps anyway.
 */
@Entity
@Table(name = "quota_usage", schema = "quota")
public class QuotaUsageEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "quota_key", nullable = false, length = 128)
    private String quotaKey;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected QuotaUsageEntity() {
        // for JPA
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQuotaKey() {
        return quotaKey;
    }

    public long getAmount() {
        return amount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
