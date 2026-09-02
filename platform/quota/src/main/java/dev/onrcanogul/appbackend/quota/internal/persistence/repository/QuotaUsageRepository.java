package dev.onrcanogul.appbackend.quota.internal.persistence.repository;

import dev.onrcanogul.appbackend.quota.internal.persistence.entity.QuotaUsageEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotaUsageRepository extends JpaRepository<QuotaUsageEntity, UUID> {

    /** Total spent inside the rolling window. Coalesced, so it returns 0 rather than null. */
    @Query("""
            select coalesce(sum(u.amount), 0)
            from QuotaUsageEntity u
            where u.userId = :userId
              and u.quotaKey = :quotaKey
              and u.occurredAt >= :since
            """)
    long sumInWindow(
            @Param("userId") UUID userId,
            @Param("quotaKey") String quotaKey,
            @Param("since") Instant since);

    @Modifying
    @Query("delete from QuotaUsageEntity u where u.occurredAt < :before")
    int deleteOlderThan(@Param("before") Instant before);

    void deleteAllByUserId(UUID userId);
}
