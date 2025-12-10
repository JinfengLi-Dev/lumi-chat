package com.lumichat.repository;

import com.lumichat.entity.OfflineMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfflineMessageRepository extends JpaRepository<OfflineMessage, Long> {

    /**
     * Find undelivered messages for a user (all devices)
     */
    @Query("SELECT om FROM OfflineMessage om " +
           "WHERE om.targetUser.id = :userId AND om.deliveredAt IS NULL " +
           "ORDER BY om.createdAt ASC")
    List<OfflineMessage> findPendingByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find undelivered messages for a specific user device
     */
    @Query("SELECT om FROM OfflineMessage om " +
           "WHERE om.targetUser.id = :userId " +
           "AND (om.targetDeviceId = :deviceId OR om.targetDeviceId IS NULL) " +
           "AND om.deliveredAt IS NULL " +
           "ORDER BY om.createdAt ASC")
    List<OfflineMessage> findPendingByUserIdAndDeviceId(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            Pageable pageable);

    /**
     * Count pending messages for a user
     */
    @Query("SELECT COUNT(om) FROM OfflineMessage om " +
           "WHERE om.targetUser.id = :userId AND om.deliveredAt IS NULL")
    long countPendingByUserId(@Param("userId") Long userId);

    /**
     * Mark messages as delivered
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE OfflineMessage om SET om.deliveredAt = CURRENT_TIMESTAMP " +
           "WHERE om.id IN :ids")
    void markAsDelivered(@Param("ids") List<Long> ids);

    /**
     * Mark all messages for a user/device as delivered
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE OfflineMessage om SET om.deliveredAt = CURRENT_TIMESTAMP " +
           "WHERE om.targetUser.id = :userId " +
           "AND (om.targetDeviceId = :deviceId OR om.targetDeviceId IS NULL) " +
           "AND om.deliveredAt IS NULL")
    void markAllDeliveredForDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    /**
     * Delete expired messages
     */
    @Modifying
    @Query("DELETE FROM OfflineMessage om " +
           "WHERE om.expiredAt IS NOT NULL AND om.expiredAt < :now")
    int deleteExpiredMessages(@Param("now") LocalDateTime now);

    /**
     * Delete delivered messages older than specified time (cleanup)
     */
    @Modifying
    @Query("DELETE FROM OfflineMessage om " +
           "WHERE om.deliveredAt IS NOT NULL AND om.deliveredAt < :before")
    int deleteDeliveredBefore(@Param("before") LocalDateTime before);

    /**
     * Check if message already queued for user
     */
    boolean existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(Long targetUserId, Long messageId);
}
