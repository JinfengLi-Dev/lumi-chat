package com.lumichat.repository;

import com.lumichat.entity.DeviceSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSyncStatusRepository extends JpaRepository<DeviceSyncStatus, Long> {

    /**
     * Find sync status for a specific user device
     */
    Optional<DeviceSyncStatus> findByUserIdAndDeviceId(Long userId, String deviceId);

    /**
     * Find all devices for a user
     */
    List<DeviceSyncStatus> findByUserId(Long userId);

    /**
     * Update last synced message ID
     */
    @Modifying
    @Query("UPDATE DeviceSyncStatus dss SET dss.lastSyncedMsgId = :msgId, " +
           "dss.lastSyncedAt = CURRENT_TIMESTAMP " +
           "WHERE dss.user.id = :userId AND dss.deviceId = :deviceId")
    void updateSyncStatus(@Param("userId") Long userId,
                          @Param("deviceId") String deviceId,
                          @Param("msgId") Long msgId);

    /**
     * Delete sync status for a device (when device is removed)
     */
    void deleteByUserIdAndDeviceId(Long userId, String deviceId);
}
