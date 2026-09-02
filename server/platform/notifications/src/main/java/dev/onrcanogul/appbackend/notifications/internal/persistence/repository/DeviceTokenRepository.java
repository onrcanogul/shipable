package dev.onrcanogul.appbackend.notifications.internal.persistence.repository;

import dev.onrcanogul.appbackend.notifications.internal.persistence.entity.DeviceTokenEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {

    Optional<DeviceTokenEntity> findByUserIdAndDeviceId(UUID userId, String deviceId);

    List<DeviceTokenEntity> findAllByUserIdAndInvalidatedAtIsNull(UUID userId);

    void deleteByUserIdAndDeviceId(UUID userId, String deviceId);

    void deleteAllByUserId(UUID userId);
}
