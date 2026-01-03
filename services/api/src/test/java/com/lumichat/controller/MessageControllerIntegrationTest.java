package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.config.PostgresTestContainerConfig;
import com.lumichat.dto.request.LoginRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MessageController.
 * Uses PostgreSQL Testcontainers because Message entity uses bigint[] arrays (atUserIds).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MessageControllerIntegrationTest extends PostgresTestContainerConfig {

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
    private User user3;
    private String user1Token;
    private String user2Token;
    private Conversation conversation;

    @BeforeEach
    void setUp() throws Exception {
        messageRepository.deleteAll();
        userConversationRepository.deleteAll();
        conversationRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");
        user3 = createUser("USER003", "user3@example.com", "User Three");

        user1Token = loginAndGetToken("user1@example.com", "device-user1");
        user2Token = loginAndGetToken("user2@example.com", "device-user2");

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
    @DisplayName("GET /conversations/{conversationId}/messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return empty list when no messages")
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            mockMvc.perform(get("/conversations/" + conversation.getId() + "/messages")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return messages for conversation")
        void shouldReturnMessagesForConversation() throws Exception {
            createMessage(conversation, user1, "Hello!");
            createMessage(conversation, user2, "Hi there!");

            mockMvc.perform(get("/conversations/" + conversation.getId() + "/messages")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() throws Exception {
            for (int i = 0; i < 5; i++) {
                createMessage(conversation, user1, "Message " + i);
            }

            mockMvc.perform(get("/conversations/" + conversation.getId() + "/messages")
                            .header("Authorization", "Bearer " + user1Token)
                            .param("limit", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/conversations/" + conversation.getId() + "/messages"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail when not a participant")
        void shouldFailWhenNotParticipant() throws Exception {
            String user3Token = loginAndGetToken("user3@example.com", "device-user3");

            mockMvc.perform(get("/conversations/" + conversation.getId() + "/messages")
                            .header("Authorization", "Bearer " + user3Token))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }

    @Nested
    @DisplayName("POST /messages")
    class SendMessageTests {

        @Test
        @DisplayName("Should send text message successfully")
        void shouldSendTextMessageSuccessfully() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hello, World!");

            mockMvc.perform(post("/messages")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.msgType").value("text"))
                    .andExpect(jsonPath("$.data.content").value("Hello, World!"))
                    .andExpect(jsonPath("$.data.senderId").value(user1.getId()));
        }

        @Test
        @DisplayName("Should send message with mentions")
        void shouldSendMessageWithMentions() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hey @User Two!");
            request.setAtUserIds(new Long[]{user2.getId()});

            mockMvc.perform(post("/messages")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content").value("Hey @User Two!"));
        }

        @Test
        @DisplayName("Should fail with missing conversation ID")
        void shouldFailWithMissingConversationId() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setMsgType("text");
            request.setContent("Hello");

            mockMvc.perform(post("/messages")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail with missing message type")
        void shouldFailWithMissingMsgType() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setContent("Hello");

            mockMvc.perform(post("/messages")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail when not a participant")
        void shouldFailWhenNotParticipant() throws Exception {
            String user3Token = loginAndGetToken("user3@example.com", "device-user3");

            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversation.getId());
            request.setMsgType("text");
            request.setContent("Hello!");

            mockMvc.perform(post("/messages")
                            .header("Authorization", "Bearer " + user3Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }

    @Nested
    @DisplayName("PUT /messages/{msgId}/recall")
    class RecallMessageTests {

        @Test
        @DisplayName("Should recall own message successfully")
        void shouldRecallOwnMessageSuccessfully() throws Exception {
            Message message = createMessage(conversation, user1, "Message to recall");

            mockMvc.perform(put("/messages/" + message.getMsgId() + "/recall")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail to recall other's message")
        void shouldFailToRecallOthersMessage() throws Exception {
            Message message = createMessage(conversation, user2, "User2's message");

            mockMvc.perform(put("/messages/" + message.getMsgId() + "/recall")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isForbidden()); // ForbiddenException -> 403
        }

        @Test
        @DisplayName("Should fail when message not found")
        void shouldFailWhenMessageNotFound() throws Exception {
            mockMvc.perform(put("/messages/nonexistent-msg-id/recall")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }

    @Nested
    @DisplayName("POST /messages/{msgId}/forward")
    class ForwardMessageTests {

        @Test
        @DisplayName("Should forward message to another conversation")
        void shouldForwardMessageToAnotherConversation() throws Exception {
            Message message = createMessage(conversation, user1, "Message to forward");
            Conversation conv2 = createPrivateConversation(user1, user3);

            String requestBody = "{\"targetConversationId\":" + conv2.getId() + "}";

            mockMvc.perform(post("/messages/" + message.getMsgId() + "/forward")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content").value("Message to forward"));
        }

        @Test
        @DisplayName("Should fail when message not found")
        void shouldFailWhenMessageNotFound() throws Exception {
            String requestBody = "{\"targetConversationId\":" + conversation.getId() + "}";

            mockMvc.perform(post("/messages/nonexistent-msg-id/forward")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }

    @Nested
    @DisplayName("DELETE /messages/{msgId}")
    class DeleteMessageTests {

        @Test
        @DisplayName("Should delete own message successfully")
        void shouldDeleteOwnMessageSuccessfully() throws Exception {
            Message message = createMessage(conversation, user1, "Message to delete");

            mockMvc.perform(delete("/messages/" + message.getMsgId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail to delete other's message")
        void shouldFailToDeleteOthersMessage() throws Exception {
            Message message = createMessage(conversation, user2, "User2's message");

            mockMvc.perform(delete("/messages/" + message.getMsgId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isForbidden()); // ForbiddenException -> 403
        }

        @Test
        @DisplayName("Should fail when message not found")
        void shouldFailWhenMessageNotFound() throws Exception {
            mockMvc.perform(delete("/messages/nonexistent-msg-id")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound()); // NotFoundException -> 404
        }
    }
}
