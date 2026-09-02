package dev.onrcanogul.appbackend.billing.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Ledger of webhook events already applied.
 *
 * <p>RevenueCat retries on any non-2xx, and delivers at-least-once even when we succeed.
 * Without this ledger a redelivered RENEWAL applies twice.
 *
 * <p>The raw payload is kept so an event that failed to process can be replayed after a
 * fix, instead of being lost.
 */
@Entity
@Table(name = "processed_webhook_event", schema = "billing")
public class ProcessedWebhookEventEntity extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "app_user_id", length = 255)
    private String appUserId;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    protected ProcessedWebhookEventEntity() {
        // for JPA
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public String getPayload() {
        return payload;
    }
}
