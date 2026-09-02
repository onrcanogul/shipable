package dev.onrcanogul.appbackend.privacy.internal.persistence.repository;

import dev.onrcanogul.appbackend.privacy.api.model.DeletionStatus;
import dev.onrcanogul.appbackend.privacy.internal.persistence.entity.DeletionRequestEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletionRequestRepository extends JpaRepository<DeletionRequestEntity, UUID> {

    Optional<DeletionRequestEntity> findByUserIdAndStatus(UUID userId, DeletionStatus status);

    Optional<DeletionRequestEntity> findFirstByUserIdOrderByRequestedAtDesc(UUID userId);

    List<DeletionRequestEntity> findAllByStatusAndScheduledForBefore(DeletionStatus status, Instant before);
}
