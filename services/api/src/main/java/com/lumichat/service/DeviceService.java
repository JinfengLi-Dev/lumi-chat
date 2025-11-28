package com.lumichat.service;

import com.lumichat.dto.response.DeviceResponse;
import com.lumichat.entity.UserDevice;
import com.lumichat.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final UserDeviceRepository userDeviceRepository;

    /**
     * Get all devices for a user
     */
    public List<DeviceResponse> getUserDevices(Long userId) {
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        return devices.stream()
                .map(DeviceResponse::from)
                .toList();
    }

    /**
     * Logout a specific device
     */
    @Transactional
    public void logoutDevice(Long userId, String deviceId) {
        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setIsOnline(false);
        device.setPushToken(null);
        userDeviceRepository.save(device);

        log.info("Logged out device {} for user {}", deviceId, userId);
    }

    /**
     * Logout all devices except the current one
     */
    @Transactional
    public void logoutAllDevices(Long userId, String currentDeviceId) {
        // Set all devices offline
        userDeviceRepository.setAllOfflineByUserId(userId);

        // Delete other devices (keep current)
        if (currentDeviceId != null && !currentDeviceId.isBlank()) {
            userDeviceRepository.deleteOtherDevices(userId, currentDeviceId);
            log.info("Logged out all devices except {} for user {}", currentDeviceId, userId);
        } else {
            log.info("Set all devices offline for user {}", userId);
        }
    }

    /**
     * Delete a specific device (remove from device list)
     */
    @Transactional
    public void deleteDevice(Long userId, String deviceId, String currentDeviceId) {
        // Cannot delete current device
        if (deviceId.equals(currentDeviceId)) {
            throw new RuntimeException("Cannot delete current device. Use logout instead.");
        }

        userDeviceRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        log.info("Deleted device {} for user {}", deviceId, userId);
    }
}
