package dev.onrcanogul.appbackend.privacy.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import dev.onrcanogul.appbackend.privacy.api.model.DeletionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One deletion request.
 *
 * <p>{@code completed_data_sets} records which contributors have already erased. Erasure
 * spans several modules and can fail halfway through - without this, a retry either
 * re-runs everything or gives up, and neither is right.
 */
@Entity
@Table(name = "deletion_request", schema = "privacy")
public class DeletionRequestEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DeletionStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_data_sets", columnDefinition = "text")
    private String completedDataSets;

    protected DeletionRequestEntity() {
        // for JPA
    }

    public UUID getUserId() {
        return userId;
    }

    public DeletionStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getCompletedDataSets() {
        return completedDataSets;
    }
}
