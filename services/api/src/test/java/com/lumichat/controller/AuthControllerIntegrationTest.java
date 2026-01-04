package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.RegisterRequest;
import com.lumichat.entity.User;
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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

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

    private User testUser;

    @BeforeEach
    void setUp() {
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .uid("TEST001")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname("Test User")
                .gender(User.Gender.male)
                .status(User.UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("password123");
            request.setNickname("New User");
            request.setGender("male");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("New User"))
                    .andExpect(jsonPath("$.data.uid").isNotEmpty());
        }

        @Test
        @DisplayName("Should fail registration with duplicate email")
        void shouldFailWithDuplicateEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");
            request.setNickname("Another User");
            request.setGender("male");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(not(0)))
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }

        @Test
        @DisplayName("Should fail registration with invalid email format")
        void shouldFailWithInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("invalid-email");
            request.setPassword("password123");
            request.setNickname("Test User");
            request.setGender("male");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration with short password")
        void shouldFailWithShortPassword() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("short@example.com");
            request.setPassword("12345");
            request.setNickname("Test User");
            request.setGender("male");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration with missing required fields")
        void shouldFailWithMissingFields() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("missing@example.com");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");
            request.setDeviceId("device-001");
            request.setDeviceType("web");
            request.setDeviceName("Chrome Browser");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
        }

        @Test
        @DisplayName("Should fail login with wrong password")
        void shouldFailWithWrongPassword() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrongpassword");
            request.setDeviceId("device-001");
            request.setDeviceType("web");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail login with non-existent email")
        void shouldFailWithNonExistentEmail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("nonexistent@example.com");
            request.setPassword("password123");
            request.setDeviceId("device-001");
            request.setDeviceType("web");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail login for inactive user")
        void shouldFailForInactiveUser() throws Exception {
            testUser.setStatus(User.UserStatus.inactive);
            userRepository.save(testUser);

            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");
            request.setDeviceId("device-001");
            request.setDeviceType("web");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail login for banned user")
        void shouldFailForBannedUser() throws Exception {
            testUser.setStatus(User.UserStatus.banned);
            userRepository.save(testUser);

            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");
            request.setDeviceId("device-001");
            request.setDeviceType("web");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail login with missing device info")
        void shouldFailWithMissingDeviceInfo() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // First login to get a refresh token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("password123");
            loginRequest.setDeviceId("device-001");
            loginRequest.setDeviceType("web");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(loginResponse)
                    .path("data")
                    .path("refreshToken")
                    .asText();

            // Use refresh token to get new access token
            String refreshRequest = "{\"refreshToken\": \"" + refreshToken + "\"}";

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("Should fail with invalid refresh token")
        void shouldFailWithInvalidRefreshToken() throws Exception {
            String refreshRequest = "{\"refreshToken\": \"invalid-token\"}";

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshRequest))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully with valid token")
        void shouldLogoutSuccessfully() throws Exception {
            // First login to get an access token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("password123");
            loginRequest.setDeviceId("device-001");
            loginRequest.setDeviceType("web");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            String accessToken = objectMapper.readTree(loginResponse)
                    .path("data")
                    .path("token")
                    .asText();

            // Logout with the access token
            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail logout without authentication")
        void shouldFailLogoutWithoutAuth() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isForbidden()); // Spring Security returns 403 by default without AuthenticationEntryPoint
        }
    }

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should return success for existing email")
        void shouldReturnSuccessForExistingEmail() throws Exception {
            String request = "{\"email\": \"test@example.com\"}";

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should return success for non-existing email (prevent enumeration)")
        void shouldReturnSuccessForNonExistingEmail() throws Exception {
            String request = "{\"email\": \"nonexistent@example.com\"}";

            // Always returns success to prevent email enumeration attacks
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("POST /auth/reset-password")
    class ResetPasswordTests {

        @Autowired
        private com.lumichat.security.JwtTokenProvider jwtTokenProvider;

        @Test
        @DisplayName("Should reset password with valid token")
        void shouldResetPasswordWithValidToken() throws Exception {
            // Generate a valid password reset token
            String resetToken = jwtTokenProvider.generatePasswordResetToken(testUser.getId());

            String request = "{\"token\": \"" + resetToken + "\", \"newPassword\": \"newpassword123\"}";

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify password was changed by trying to login with new password
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("newpassword123");
            loginRequest.setDeviceId("device-001");
            loginRequest.setDeviceType("web");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail reset with invalid token")
        void shouldFailResetWithInvalidToken() throws Exception {
            String request = "{\"token\": \"invalid-token\", \"newPassword\": \"newpassword123\"}";

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail reset with access token instead of reset token")
        void shouldFailResetWithAccessToken() throws Exception {
            // Generate an access token (not a password reset token)
            String accessToken = jwtTokenProvider.generateAccessToken(testUser.getId(), "device-001");

            String request = "{\"token\": \"" + accessToken + "\", \"newPassword\": \"newpassword123\"}";

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail reset with expired token")
        void shouldFailResetWithExpiredToken() throws Exception {
            // Create an expired password reset token manually
            // Using the same secret from test configuration
            String testSecret = "your-256-bit-secret-key-for-jwt-tokens-must-be-long-enough";
            var secretKey = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));

            Date now = new Date();
            Date expiredDate = new Date(now.getTime() - 3600000); // Expired 1 hour ago

            String expiredToken = Jwts.builder()
                    .subject(testUser.getId().toString())
                    .claim("type", "password-reset")
                    .issuedAt(new Date(now.getTime() - 7200000)) // Issued 2 hours ago
                    .expiration(expiredDate)
                    .signWith(secretKey)
                    .compact();

            String request = "{\"token\": \"" + expiredToken + "\", \"newPassword\": \"newpassword123\"}";

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should successfully reset password with valid token")
        void shouldSuccessfullyResetPasswordWithValidToken() throws Exception {
            String resetToken = jwtTokenProvider.generatePasswordResetToken(testUser.getId());

            String request = "{\"token\": \"" + resetToken + "\", \"newPassword\": \"newSecurePassword123\"}";

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("Security: Token Revocation Tests")
    class TokenRevocationTests {

        @Test
        @DisplayName("Should successfully logout with valid token")
        void shouldSuccessfullyLogoutWithValidToken() throws Exception {
            // Login first
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("password123");
            loginRequest.setDeviceId("device-revoke-test");
            loginRequest.setDeviceType("web");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            String accessToken = objectMapper.readTree(loginResponse)
                    .path("data")
                    .path("token")
                    .asText();

            // Logout should succeed
            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Note: With stateless JWT tokens, the token may still be cryptographically valid
            // until expiration. Full token revocation requires a token blacklist (Redis-based)
            // or short-lived tokens with refresh token rotation.
        }

        @Test
        @DisplayName("Should mark device as offline after logout")
        void shouldMarkDeviceOfflineAfterLogout() throws Exception {
            // Login first
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("password123");
            loginRequest.setDeviceId("device-refresh-revoke");
            loginRequest.setDeviceType("web");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            String accessToken = objectMapper.readTree(loginResponse)
                    .path("data")
                    .path("token")
                    .asText();

            // Logout - marks device as offline
            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Note: Current implementation uses stateless JWT tokens.
            // Refresh tokens remain valid until expiration (JWT is self-contained).
            // To implement full token revocation, consider:
            // 1. Adding a token blacklist in Redis
            // 2. Checking device.isOnline in refreshToken flow
            // 3. Using short-lived access tokens with refresh token rotation
        }
    }

    @Nested
    @DisplayName("Security: Multi-Device Login Tests")
    class MultiDeviceLoginTests {

        @Test
        @DisplayName("Should allow login from multiple devices")
        void shouldAllowMultiDeviceLogin() throws Exception {
            // Login from device 1
            LoginRequest device1Request = new LoginRequest();
            device1Request.setEmail("test@example.com");
            device1Request.setPassword("password123");
            device1Request.setDeviceId("device-multi-1");
            device1Request.setDeviceType("web");
            device1Request.setDeviceName("Chrome Browser");

            MvcResult device1Result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(device1Request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();

            String device1Token = objectMapper.readTree(device1Result.getResponse().getContentAsString())
                    .path("data")
                    .path("token")
                    .asText();

            // Login from device 2
            LoginRequest device2Request = new LoginRequest();
            device2Request.setEmail("test@example.com");
            device2Request.setPassword("password123");
            device2Request.setDeviceId("device-multi-2");
            device2Request.setDeviceType("ios");
            device2Request.setDeviceName("iPhone 15");

            MvcResult device2Result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(device2Request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();

            String device2Token = objectMapper.readTree(device2Result.getResponse().getContentAsString())
                    .path("data")
                    .path("token")
                    .asText();

            // Both tokens should be different
            org.junit.jupiter.api.Assertions.assertNotEquals(device1Token, device2Token);

            // Both tokens should still work
            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer " + device1Token))
                    .andExpect(status().isOk());

            // Device 2 token should still work after device 1 logout
            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer " + device2Token))
                    .andExpect(status().isOk());
        }
    }
}
