package dev.onrcanogul.appbackend.identity.internal.persistence.repository;

import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.internal.persistence.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByProviderAndExternalSubject(AuthProvider provider, String externalSubject);

    Optional<UserEntity> findByDeviceId(String deviceId);
}
