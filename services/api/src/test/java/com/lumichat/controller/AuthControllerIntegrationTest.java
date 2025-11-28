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

import java.time.LocalDateTime;

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
}
