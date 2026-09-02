package dev.onrcanogul.appbackend.appconfig.internal.persistence.repository;

import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.AppSettingEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSettingEntity, UUID> {

    Optional<AppSettingEntity> findBySettingKey(String settingKey);

    void deleteBySettingKey(String settingKey);
}
