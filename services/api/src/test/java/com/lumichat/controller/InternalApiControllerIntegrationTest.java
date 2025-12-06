package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.config.PostgresTestContainerConfig;
import com.lumichat.dto.request.SendMessageRequest;
import com.lumichat.entity.Conversation;
import com.lumichat.entity.Message;
import com.lumichat.entity.User;
import com.lumichat.entity.UserConversation;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.repository.MessageRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InternalApiController.
 * Tests internal service-to-service API endpoints used by the IM server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalApiControllerIntegrationTest extends PostgresTestContainerConfig {

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
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        userConversationRepository.deleteAll();
        conversationRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");
        conversation = createPrivateConversation(user1, user2);
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

    private Conversation createPrivateConversation(User user, User targetUser) {
        Conversation conv = Conversation.builder()
                .type(Conversation.ConversationType.private_chat)
                .participantIds(new Long[]{user.getId(), targetUser.getId()})
                .createdAt(LocalDateTime.now())
                .build();
        conv = conversationRepository.save(conv);

        UserConversation uc1 = UserConversation.builder()
                .user(user)
                .conversation(conv)
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userConversationRepository.save(uc1);

        UserConversation uc2 = UserConversation.builder()
                .user(targetUser)
                .conversation(conv)
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userConversationRepository.save(uc2);

        return conv;
    }

    private Message createMessage(Conversation conv, User sender, String content) {
        Message message = Message.builder()
                .msgId(UUID.randomUUID().toString())
                .conversation(conv)
                .sender(sender)
                .senderDeviceId("test-device")
                .msgType(Message.MessageType.text)
                .content(content)
                .serverCreatedAt(LocalDateTime.now())
                .build();
        return messageRepository.save(message);
    }

    @Nested
    @DisplayName("POST /internal/messages")
    class PersistMessageTests {

        @Test
        @DisplayName("Should persist message successfully with valid internal service header")
        void shouldPersistMessageSuccessfully() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hello from IM server!");

            mockMvc.perform(post("/internal/messages")
                            .header("X-Internal-Service", "im-server")
                            .header("X-User-Id", user1.getId().toString())
                            .header("X-Device-Id", "device-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.msgId").isNotEmpty())
                    .andExpect(jsonPath("$.data.content").value("Hello from IM server!"))
                    .andExpect(jsonPath("$.data.msgType").value("text"))
                    .andExpect(jsonPath("$.data.senderId").value(user1.getId()));
        }

        @Test
        @DisplayName("Should fail without internal service header")
        void shouldFailWithoutInternalServiceHeader() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hello!");

            mockMvc.perform(post("/internal/messages")
                            .header("X-User-Id", user1.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail with unauthorized service name")
        void shouldFailWithUnauthorizedService() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hello!");

            mockMvc.perform(post("/internal/messages")
                            .header("X-Internal-Service", "unauthorized-service")
                            .header("X-User-Id", user1.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail without user ID header")
        void shouldFailWithoutUserId() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hello!");

            mockMvc.perform(post("/internal/messages")
                            .header("X-Internal-Service", "im-server")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Nested
    @DisplayName("PUT /internal/messages/{msgId}/recall")
    class RecallMessageTests {

        @Test
        @DisplayName("Should recall message successfully")
        void shouldRecallMessageSuccessfully() throws Exception {
            Message message = createMessage(conversation, user1, "Message to recall");

            mockMvc.perform(put("/internal/messages/" + message.getMsgId() + "/recall")
                            .header("X-Internal-Service", "im-server")
                            .header("X-User-Id", user1.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail to recall other's message")
        void shouldFailToRecallOthersMessage() throws Exception {
            Message message = createMessage(conversation, user2, "User2's message");

            mockMvc.perform(put("/internal/messages/" + message.getMsgId() + "/recall")
                            .header("X-Internal-Service", "im-server")
                            .header("X-User-Id", user1.getId().toString()))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /internal/conversations/{conversationId}/participants")
    class GetParticipantsTests {

        @Test
        @DisplayName("Should return conversation participants")
        void shouldReturnConversationParticipants() throws Exception {
            mockMvc.perform(get("/internal/conversations/" + conversation.getId() + "/participants")
                            .header("X-Internal-Service", "im-server"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data", containsInAnyOrder(user1.getId().intValue(), user2.getId().intValue())));
        }

        @Test
        @DisplayName("Should return 404 for non-existent conversation")
        void shouldReturn404ForNonExistentConversation() throws Exception {
            mockMvc.perform(get("/internal/conversations/99999/participants")
                            .header("X-Internal-Service", "im-server"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    @Nested
    @DisplayName("GET /internal/conversations/{conversationId}/messages")
    class GetMessagesForSyncTests {

        @Test
        @DisplayName("Should return messages for sync")
        void shouldReturnMessagesForSync() throws Exception {
            createMessage(conversation, user1, "Message 1");
            createMessage(conversation, user2, "Message 2");
            createMessage(conversation, user1, "Message 3");

            mockMvc.perform(get("/internal/conversations/" + conversation.getId() + "/messages")
                            .header("X-Internal-Service", "im-server")
                            .header("X-User-Id", user1.getId().toString())
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(3)));
        }

        @Test
        @DisplayName("Should return empty list when no messages")
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            mockMvc.perform(get("/internal/conversations/" + conversation.getId() + "/messages")
                            .header("X-Internal-Service", "im-server")
                            .header("X-User-Id", user1.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() throws Exception {
            for (int i = 0; i < 10; i++) {
                createMessage(conversation, user1, "Message " + i);
            }

            mockMvc.perform(get("/internal/conversations/" + conversation.getId() + "/messages")
                            .header("X-Internal-Service", "im-server")
                            .header("X-User-Id", user1.getId().toString())
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(5)));
        }
    }
}
