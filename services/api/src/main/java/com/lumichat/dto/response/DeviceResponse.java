package com.lumichat.dto.response;

import com.lumichat.entity.UserDevice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceResponse {

    private Long id;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private Boolean isOnline;
    private LocalDateTime lastActiveAt;
    private String lastIp;
    private LocalDateTime createdAt;

    public static DeviceResponse from(UserDevice device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceType(device.getDeviceType().name())
                .deviceName(device.getDeviceName())
                .isOnline(device.getIsOnline())
                .lastActiveAt(device.getLastActiveAt())
                .lastIp(device.getLastIp())
                .createdAt(device.getCreatedAt())
                .build();
    }
}
