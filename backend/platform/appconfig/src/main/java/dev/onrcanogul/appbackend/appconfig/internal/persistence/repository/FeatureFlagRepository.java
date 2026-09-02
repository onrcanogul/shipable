package dev.onrcanogul.appbackend.appconfig.internal.persistence.repository;

import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.FeatureFlagEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, UUID> {

    Optional<FeatureFlagEntity> findByFlagKey(String flagKey);

    List<FeatureFlagEntity> findAllByExposedToClientTrue();
}
