package com.lumichat.controller;

import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.DeviceResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Get all devices for current user
     * GET /devices
     */
    @GetMapping
    public ApiResponse<List<DeviceResponse>> getDevices(@AuthenticationPrincipal UserPrincipal principal) {
        List<DeviceResponse> devices = deviceService.getUserDevices(principal.getId());
        return ApiResponse.success(devices);
    }

    /**
     * Logout a specific device
     * DELETE /devices/{deviceId}
     */
    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> logoutDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String deviceId) {
        deviceService.deleteDevice(principal.getId(), deviceId, principal.getDeviceId());
        return ApiResponse.success();
    }

    /**
     * Logout all devices except current
     * DELETE /devices
     */
    @DeleteMapping
    public ApiResponse<Void> logoutAllDevices(@AuthenticationPrincipal UserPrincipal principal) {
        deviceService.logoutAllDevices(principal.getId(), principal.getDeviceId());
        return ApiResponse.success();
    }
}
