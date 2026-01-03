package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.entity.User;
import com.lumichat.entity.UserDevice;
import com.lumichat.repository.UserDeviceRepository;
import com.lumichat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeviceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private String user1Token;
    private String user1DeviceId;

    @BeforeEach
    void setUp() throws Exception {
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");

        // Login creates a device automatically
        user1DeviceId = "test-device-web";
        user1Token = loginAndGetToken("user1@example.com", user1DeviceId);
    }

    private User createUser(String uid, String email, String nickname) {
        User user = User.builder()
                .uid(uid)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname(nickname)
                .gender(User.Gender.male)
                .status(User.UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private String loginAndGetToken(String email, String deviceId) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setDeviceId(deviceId);
        request.setDeviceType("web");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    @Nested
    @DisplayName("GET /devices")
    class GetDevicesTests {

        @Test
        @DisplayName("Should return user's devices")
        void shouldReturnUserDevices() throws Exception {
            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.data[0].deviceId").value(user1DeviceId));
        }

        @Test
        @DisplayName("Should return multiple devices when user logs in from multiple")
        void shouldReturnMultipleDevices() throws Exception {
            // Login from a second device
            loginAndGetToken("user1@example.com", "test-device-ios");

            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/devices"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /devices/{deviceId}")
    class LogoutDeviceTests {

        @Test
        @DisplayName("Should logout specific device")
        void shouldLogoutSpecificDevice() throws Exception {
            // Login from a second device
            String secondDeviceId = "test-device-mobile";
            loginAndGetToken("user1@example.com", secondDeviceId);

            // Verify we have 2 devices
            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));

            // Logout the second device
            mockMvc.perform(delete("/devices/" + secondDeviceId)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify only 1 device remains
            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].deviceId").value(user1DeviceId));
        }

        @Test
        @DisplayName("Should fail when trying to logout current device")
        void shouldFailWhenLogoutCurrentDevice() throws Exception {
            // This returns 400 Bad Request - deleting current device should use logout instead
            mockMvc.perform(delete("/devices/" + user1DeviceId)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isBadRequest()); // BadRequestException -> 400
        }

        @Test
        @DisplayName("Should succeed silently when device not found (no-op delete)")
        void shouldSucceedWhenDeviceNotFound() throws Exception {
            // JPA's deleteBy... methods are no-ops if nothing matches
            mockMvc.perform(delete("/devices/nonexistent-device")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /devices")
    class LogoutAllDevicesTests {

        @Test
        @DisplayName("Should logout all devices except current")
        void shouldLogoutAllDevicesExceptCurrent() throws Exception {
            // Login from multiple devices
            loginAndGetToken("user1@example.com", "test-device-ios");
            loginAndGetToken("user1@example.com", "test-device-android");

            // Verify we have 3 devices
            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)));

            // Logout all devices except current
            mockMvc.perform(delete("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify only current device remains
            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].deviceId").value(user1DeviceId));
        }

        @Test
        @DisplayName("Should succeed even when only current device exists")
        void shouldSucceedWhenOnlyCurrentDevice() throws Exception {
            mockMvc.perform(delete("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify current device still exists
            mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }
}
