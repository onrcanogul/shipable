package dev.onrcanogul.appbackend.billing.internal.persistence.repository;

import dev.onrcanogul.appbackend.billing.internal.persistence.entity.EntitlementSnapshotEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementSnapshotRepository extends JpaRepository<EntitlementSnapshotEntity, UUID> {

    List<EntitlementSnapshotEntity> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}
