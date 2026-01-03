package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.config.PostgresTestContainerConfig;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.entity.Conversation;
import com.lumichat.entity.User;
import com.lumichat.entity.UserConversation;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.repository.UserConversationRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ConversationController.
 * Uses PostgreSQL Testcontainers because Conversation entity uses bigint[] arrays.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConversationControllerIntegrationTest extends PostgresTestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserConversationRepository userConversationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private User user3;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        userConversationRepository.deleteAll();
        conversationRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");
        user3 = createUser("USER003", "user3@example.com", "User Three");

        user1Token = loginAndGetToken("user1@example.com", "device-user1");
        user2Token = loginAndGetToken("user2@example.com", "device-user2");
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
    @DisplayName("GET /conversations")
    class GetConversationsTests {

        @Test
        @DisplayName("Should return empty list when user has no conversations")
        void shouldReturnEmptyListWhenNoConversations() throws Exception {
            mockMvc.perform(get("/conversations")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return user's conversations")
        void shouldReturnUserConversations() throws Exception {
            createPrivateConversation(user1, user2);

            mockMvc.perform(get("/conversations")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].type").value("private_chat"));
        }

        @Test
        @DisplayName("Should exclude hidden conversations")
        void shouldExcludeHiddenConversations() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);
            uc.setIsHidden(true);
            userConversationRepository.save(uc);

            mockMvc.perform(get("/conversations")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should order by pinned first then by time")
        void shouldOrderByPinnedThenTime() throws Exception {
            // Create two conversations
            UserConversation uc1 = createPrivateConversation(user1, user2);
            UserConversation uc2 = createPrivateConversationWithUser3();

            // Pin the second one
            uc2.setIsPinned(true);
            userConversationRepository.save(uc2);

            mockMvc.perform(get("/conversations")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].isPinned").value(true));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/conversations"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /conversations/{id}")
    class GetConversationTests {

        @Test
        @DisplayName("Should get conversation details")
        void shouldGetConversationDetails() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);

            mockMvc.perform(get("/conversations/" + uc.getConversation().getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(uc.getConversation().getId()))
                    .andExpect(jsonPath("$.data.type").value("private_chat"));
        }

        @Test
        @DisplayName("Should fail when conversation not found")
        void shouldFailWhenConversationNotFound() throws Exception {
            mockMvc.perform(get("/conversations/99999")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }

        @Test
        @DisplayName("Should fail when not a participant")
        void shouldFailWhenNotParticipant() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);
            String user3Token = loginAndGetToken("user3@example.com", "device-user3");

            mockMvc.perform(get("/conversations/" + uc.getConversation().getId())
                            .header("Authorization", "Bearer " + user3Token))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }

    @Nested
    @DisplayName("POST /conversations/private")
    class CreatePrivateConversationTests {

        @Test
        @DisplayName("Should create private conversation successfully")
        void shouldCreatePrivateConversationSuccessfully() throws Exception {
            String requestBody = "{\"targetUserId\":" + user2.getId() + "}";

            mockMvc.perform(post("/conversations/private")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.type").value("private_chat"))
                    .andExpect(jsonPath("$.data.targetUser.id").value(user2.getId()));
        }

        @Test
        @DisplayName("Should return existing conversation if already exists")
        void shouldReturnExistingConversation() throws Exception {
            String requestBody = "{\"targetUserId\":" + user2.getId() + "}";

            // Create first
            MvcResult result1 = mockMvc.perform(post("/conversations/private")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andReturn();

            Long firstId = objectMapper.readTree(result1.getResponse().getContentAsString())
                    .path("data").path("id").asLong();

            // Create second (should return same conversation)
            MvcResult result2 = mockMvc.perform(post("/conversations/private")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andReturn();

            Long secondId = objectMapper.readTree(result2.getResponse().getContentAsString())
                    .path("data").path("id").asLong();

            assertEquals(firstId, secondId, "Should return the same conversation");
        }

        @Test
        @DisplayName("Should fail when target user not found")
        void shouldFailWhenTargetUserNotFound() throws Exception {
            String requestBody = "{\"targetUserId\":99999}";

            mockMvc.perform(post("/conversations/private")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }

    @Nested
    @DisplayName("DELETE /conversations/{id}")
    class DeleteConversationTests {

        @Test
        @DisplayName("Should delete conversation successfully (soft delete)")
        void shouldDeleteConversationSuccessfully() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);

            mockMvc.perform(delete("/conversations/" + uc.getConversation().getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify conversation is hidden (soft delete)
            mockMvc.perform(get("/conversations")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /conversations/{id}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark conversation as read")
        void shouldMarkConversationAsRead() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);
            uc.setUnreadCount(5);
            userConversationRepository.save(uc);

            mockMvc.perform(post("/conversations/" + uc.getConversation().getId() + "/read")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /conversations/{id}/mute")
    class ToggleMuteTests {

        @Test
        @DisplayName("Should mute conversation")
        void shouldMuteConversation() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);

            mockMvc.perform(put("/conversations/" + uc.getConversation().getId() + "/mute")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"muted\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should unmute conversation")
        void shouldUnmuteConversation() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);
            uc.setIsMuted(true);
            userConversationRepository.save(uc);

            mockMvc.perform(put("/conversations/" + uc.getConversation().getId() + "/mute")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"muted\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /conversations/{id}/pin")
    class TogglePinTests {

        @Test
        @DisplayName("Should pin conversation")
        void shouldPinConversation() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);

            mockMvc.perform(put("/conversations/" + uc.getConversation().getId() + "/pin")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pinned\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should unpin conversation")
        void shouldUnpinConversation() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);
            uc.setIsPinned(true);
            userConversationRepository.save(uc);

            mockMvc.perform(put("/conversations/" + uc.getConversation().getId() + "/pin")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pinned\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /conversations/{id}/draft")
    class SaveDraftTests {

        @Test
        @DisplayName("Should save draft")
        void shouldSaveDraft() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);

            mockMvc.perform(put("/conversations/" + uc.getConversation().getId() + "/draft")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"draft\":\"Hello, this is my draft message...\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should clear draft")
        void shouldClearDraft() throws Exception {
            UserConversation uc = createPrivateConversation(user1, user2);
            uc.setDraft("Previous draft");
            userConversationRepository.save(uc);

            mockMvc.perform(put("/conversations/" + uc.getConversation().getId() + "/draft")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"draft\":null}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    private UserConversation createPrivateConversation(User user, User targetUser) {
        Conversation conversation = Conversation.builder()
                .type(Conversation.ConversationType.private_chat)
                .participantIds(new Long[]{user.getId(), targetUser.getId()})
                .createdAt(LocalDateTime.now())
                .build();
        conversation = conversationRepository.save(conversation);

        UserConversation userConversation = UserConversation.builder()
                .user(user)
                .conversation(conversation)
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userConversationRepository.save(userConversation);
    }

    private UserConversation createPrivateConversationWithUser3() {
        Conversation conversation = Conversation.builder()
                .type(Conversation.ConversationType.private_chat)
                .participantIds(new Long[]{user1.getId(), user3.getId()})
                .createdAt(LocalDateTime.now())
                .build();
        conversation = conversationRepository.save(conversation);

        UserConversation userConversation = UserConversation.builder()
                .user(user1)
                .conversation(conversation)
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userConversationRepository.save(userConversation);
    }
}
