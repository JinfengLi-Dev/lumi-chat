package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.ChangePasswordRequest;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.UpdateProfileRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

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
    private User testUser2;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .uid("LCTEST001")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname("Test User")
                .gender(User.Gender.male)
                .signature("Hello World")
                .status(User.UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        testUser2 = User.builder()
                .uid("LCTEST002")
                .email("test2@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname("Test User 2")
                .gender(User.Gender.female)
                .status(User.UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        testUser2 = userRepository.save(testUser2);

        // Login to get access token
        accessToken = loginAndGetToken();
    }

    private String loginAndGetToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setDeviceId("device-001");
        loginRequest.setDeviceType("web");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Nested
    @DisplayName("GET /users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user profile")
        void shouldGetCurrentUser() throws Exception {
            mockMvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("Test User"))
                    .andExpect(jsonPath("$.data.uid").value("LCTEST001"));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /users/me")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update profile successfully")
        void shouldUpdateProfile() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("Updated Nickname");
            request.setGender("female");
            request.setSignature("New signature");

            mockMvc.perform(put("/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.nickname").value("Updated Nickname"))
                    .andExpect(jsonPath("$.data.signature").value("New signature"));
        }

        @Test
        @DisplayName("Should update partial profile")
        void shouldUpdatePartialProfile() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setSignature("Only signature updated");

            mockMvc.perform(put("/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.nickname").value("Test User")) // Unchanged
                    .andExpect(jsonPath("$.data.signature").value("Only signature updated"));
        }

        @Test
        @DisplayName("Should fail with invalid nickname length")
        void shouldFailWithInvalidNickname() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("X"); // Too short

            mockMvc.perform(put("/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("New Name");

            mockMvc.perform(put("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /users/me/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePassword() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("password123");
            request.setNewPassword("newPassword123");

            mockMvc.perform(put("/users/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify can login with new password
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("newPassword123");
            loginRequest.setDeviceId("device-002");
            loginRequest.setDeviceType("web");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail with wrong current password")
        void shouldFailWithWrongCurrentPassword() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("newPassword123");

            mockMvc.perform(put("/users/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail with short new password")
        void shouldFailWithShortNewPassword() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("password123");
            request.setNewPassword("12345"); // Too short

            mockMvc.perform(put("/users/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("password123");
            request.setNewPassword("newPassword123");

            mockMvc.perform(put("/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /users/search")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users by email")
        void shouldSearchByEmail() throws Exception {
            mockMvc.perform(get("/users/search")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("q", "test2@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].email").value("test2@example.com"));
        }

        @Test
        @DisplayName("Should search users by UID")
        void shouldSearchByUid() throws Exception {
            mockMvc.perform(get("/users/search")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("q", "LCTEST002"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].uid").value("LCTEST002"));
        }

        @Test
        @DisplayName("Should return empty for no matches")
        void shouldReturnEmptyForNoMatches() throws Exception {
            mockMvc.perform(get("/users/search")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("q", "nonexistent@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/users/search")
                            .param("q", "test"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /users/{uid}")
    class GetUserByUidTests {

        @Test
        @DisplayName("Should get user by UID")
        void shouldGetUserByUid() throws Exception {
            mockMvc.perform(get("/users/LCTEST002")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.uid").value("LCTEST002"))
                    .andExpect(jsonPath("$.data.nickname").value("Test User 2"));
        }

        @Test
        @DisplayName("Should fail for non-existent UID")
        void shouldFailForNonExistentUid() throws Exception {
            mockMvc.perform(get("/users/NONEXISTENT")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/users/LCTEST002"))
                    .andExpect(status().isForbidden());
        }
    }
}
