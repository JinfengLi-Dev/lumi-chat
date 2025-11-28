package com.lumichat.service;

import com.lumichat.dto.response.DeviceResponse;
import com.lumichat.entity.User;
import com.lumichat.entity.UserDevice;
import com.lumichat.repository.UserDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceService Tests")
class DeviceServiceTest {

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    private User testUser;
    private UserDevice testDevice;
    private UserDevice otherDevice;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .nickname("TestUser")
                .build();

        testDevice = UserDevice.builder()
                .id(1L)
                .user(testUser)
                .deviceId("device-123")
                .deviceType(UserDevice.DeviceType.web)
                .deviceName("Chrome Browser")
                .isOnline(true)
                .pushToken("push-token-123")
                .createdAt(LocalDateTime.now())
                .build();

        otherDevice = UserDevice.builder()
                .id(2L)
                .user(testUser)
                .deviceId("device-456")
                .deviceType(UserDevice.DeviceType.ios)
                .deviceName("iPhone 15")
                .isOnline(true)
                .pushToken("push-token-456")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GetUserDevices Tests")
    class GetUserDevicesTests {

        @Test
        @DisplayName("Should get all user devices successfully")
        void shouldGetAllUserDevicesSuccessfully() {
            // Given
            when(userDeviceRepository.findByUserId(1L))
                    .thenReturn(Arrays.asList(testDevice, otherDevice));

            // When
            List<DeviceResponse> results = deviceService.getUserDevices(1L);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no devices")
        void shouldReturnEmptyListWhenNoDevices() {
            // Given
            when(userDeviceRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<DeviceResponse> results = deviceService.getUserDevices(1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("LogoutDevice Tests")
    class LogoutDeviceTests {

        @Test
        @DisplayName("Should logout device successfully")
        void shouldLogoutDeviceSuccessfully() {
            // Given
            when(userDeviceRepository.findByUserIdAndDeviceId(1L, "device-123"))
                    .thenReturn(Optional.of(testDevice));
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            deviceService.logoutDevice(1L, "device-123");

            // Then
            ArgumentCaptor<UserDevice> deviceCaptor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(deviceCaptor.capture());

            UserDevice savedDevice = deviceCaptor.getValue();
            assertThat(savedDevice.getIsOnline()).isFalse();
            assertThat(savedDevice.getPushToken()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when device not found")
        void shouldThrowExceptionWhenDeviceNotFound() {
            // Given
            when(userDeviceRepository.findByUserIdAndDeviceId(1L, "device-not-found"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> deviceService.logoutDevice(1L, "device-not-found"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Device not found");
        }
    }

    @Nested
    @DisplayName("LogoutAllDevices Tests")
    class LogoutAllDevicesTests {

        @Test
        @DisplayName("Should logout all devices except current")
        void shouldLogoutAllDevicesExceptCurrent() {
            // When
            deviceService.logoutAllDevices(1L, "device-123");

            // Then
            verify(userDeviceRepository).setAllOfflineByUserId(1L);
            verify(userDeviceRepository).deleteOtherDevices(1L, "device-123");
        }

        @Test
        @DisplayName("Should only set offline when no current device")
        void shouldOnlySetOfflineWhenNoCurrentDevice() {
            // When
            deviceService.logoutAllDevices(1L, null);

            // Then
            verify(userDeviceRepository).setAllOfflineByUserId(1L);
            verify(userDeviceRepository, never()).deleteOtherDevices(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should only set offline when current device is blank")
        void shouldOnlySetOfflineWhenCurrentDeviceIsBlank() {
            // When
            deviceService.logoutAllDevices(1L, "   ");

            // Then
            verify(userDeviceRepository).setAllOfflineByUserId(1L);
            verify(userDeviceRepository, never()).deleteOtherDevices(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("DeleteDevice Tests")
    class DeleteDeviceTests {

        @Test
        @DisplayName("Should delete device successfully")
        void shouldDeleteDeviceSuccessfully() {
            // When
            deviceService.deleteDevice(1L, "device-456", "device-123");

            // Then
            verify(userDeviceRepository).deleteByUserIdAndDeviceId(1L, "device-456");
        }

        @Test
        @DisplayName("Should throw exception when trying to delete current device")
        void shouldThrowExceptionWhenDeletingCurrentDevice() {
            // When/Then
            assertThatThrownBy(() -> deviceService.deleteDevice(1L, "device-123", "device-123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot delete current device. Use logout instead.");

            verify(userDeviceRepository, never()).deleteByUserIdAndDeviceId(anyLong(), anyString());
        }
    }
}
