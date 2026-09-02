package dev.onrcanogul.appbackend.appconfig.internal.persistence.repository;

import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.PlatformConfigEntity;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfigEntity, UUID> {

    Optional<PlatformConfigEntity> findByPlatform(ClientPlatform platform);
}
