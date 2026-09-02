package dev.onrcanogul.appbackend.billing.internal.persistence.repository;

import dev.onrcanogul.appbackend.billing.internal.persistence.entity.ProcessedWebhookEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEventEntity, UUID> {

    boolean existsByEventId(String eventId);
}
