package dev.onrcanogul.appbackend.identity.internal.persistence.repository;

import dev.onrcanogul.appbackend.identity.internal.persistence.entity.RefreshTokenEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /** Housekeeping: expired tokens are dead weight and a needless liability. */
    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
