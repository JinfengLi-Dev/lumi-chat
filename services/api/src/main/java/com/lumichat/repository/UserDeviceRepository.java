package com.lumichat.repository;

import com.lumichat.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findByUserId(Long userId);

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Query("UPDATE UserDevice d SET d.isOnline = false WHERE d.user.id = :userId")
    void setAllOfflineByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserDevice d WHERE d.user.id = :userId AND d.deviceId != :currentDeviceId")
    void deleteOtherDevices(Long userId, String currentDeviceId);

    void deleteByUserIdAndDeviceId(Long userId, String deviceId);
}
