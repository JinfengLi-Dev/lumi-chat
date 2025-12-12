package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.config.PostgresTestContainerConfig;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.entity.*;
import com.lumichat.repository.*;
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
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SyncControllerIntegrationTest extends PostgresTestContainerConfig {

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
    private OfflineMessageRepository offlineMessageRepository;

    @Autowired
    private DeviceSyncStatusRepository deviceSyncStatusRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private String user1Token;
    private Conversation conversation;

    @BeforeEach
    void setUp() throws Exception {
        offlineMessageRepository.deleteAll();
        deviceSyncStatusRepository.deleteAll();
        messageRepository.deleteAll();
        userConversationRepository.deleteAll();
        conversationRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");

        user1Token = loginAndGetToken("user1@example.com", "device-user1");

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

    private OfflineMessage createOfflineMessage(User targetUser, String deviceId, Message message) {
        OfflineMessage offlineMessage = OfflineMessage.builder()
                .targetUser(targetUser)
                .targetDeviceId(deviceId)
                .message(message)
                .conversation(message.getConversation())
                .expiredAt(LocalDateTime.now().plusDays(7))
                .retryCount(0)
                .build();
        return offlineMessageRepository.save(offlineMessage);
    }

    @Nested
    @DisplayName("GET /sync/messages")
    class GetPendingMessagesTests {

        @Test
        @DisplayName("Should return pending messages for user")
        void shouldReturnPendingMessages() throws Exception {
            // Given
            Message msg1 = createMessage(conversation, user2, "Hello from user2");
            Message msg2 = createMessage(conversation, user2, "Another message");
            createOfflineMessage(user1, "device-user1", msg1);
            createOfflineMessage(user1, "device-user1", msg2);

            // When/Then
            mockMvc.perform(get("/sync/messages")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalPending").value(2))
                    .andExpect(jsonPath("$.data.hasMore").value(false));
        }

        @Test
        @DisplayName("Should return empty list when no pending messages")
        void shouldReturnEmptyListWhenNoPending() throws Exception {
            mockMvc.perform(get("/sync/messages")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalPending").value(0))
                    .andExpect(jsonPath("$.data.hasMore").value(false));
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() throws Exception {
            // Given - Create 5 offline messages
            for (int i = 0; i < 5; i++) {
                Message msg = createMessage(conversation, user2, "Message " + i);
                createOfflineMessage(user1, "device-user1", msg);
            }

            // When/Then
            mockMvc.perform(get("/sync/messages")
                            .param("limit", "3")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.messages", hasSize(3)))
                    .andExpect(jsonPath("$.data.totalPending").value(5))
                    .andExpect(jsonPath("$.data.hasMore").value(true));
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(get("/sync/messages"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /sync/ack")
    class AcknowledgeMessagesTests {

        @Test
        @DisplayName("Should mark specific messages as delivered")
        void shouldMarkSpecificMessagesAsDelivered() throws Exception {
            // Given
            Message msg = createMessage(conversation, user2, "Test message");
            OfflineMessage offlineMsg = createOfflineMessage(user1, "device-user1", msg);

            SyncController.SyncAckRequest request = new SyncController.SyncAckRequest(
                    Arrays.asList(offlineMsg.getId()),
                    null,
                    false
            );

            // When/Then
            mockMvc.perform(post("/sync/ack")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify message is marked as delivered
            OfflineMessage updated = offlineMessageRepository.findById(offlineMsg.getId()).orElseThrow();
            assertThat(updated.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark all messages as delivered")
        void shouldMarkAllMessagesAsDelivered() throws Exception {
            // Given
            Message msg1 = createMessage(conversation, user2, "Message 1");
            Message msg2 = createMessage(conversation, user2, "Message 2");
            createOfflineMessage(user1, "device-user1", msg1);
            createOfflineMessage(user1, "device-user1", msg2);

            SyncController.SyncAckRequest request = new SyncController.SyncAckRequest(
                    null,
                    null,
                    true
            );

            // When/Then
            mockMvc.perform(post("/sync/ack")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify all messages are marked as delivered
            long pending = offlineMessageRepository.countPendingByUserId(user1.getId());
            assertThat(pending).isZero();
        }

        @Test
        @DisplayName("Should update sync status with lastMessageId")
        void shouldUpdateSyncStatusWithLastMessageId() throws Exception {
            // Given
            SyncController.SyncAckRequest request = new SyncController.SyncAckRequest(
                    null,
                    100L,
                    false
            );

            // When/Then
            mockMvc.perform(post("/sync/ack")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify sync status was created/updated
            var syncStatus = deviceSyncStatusRepository.findByUserIdAndDeviceId(user1.getId(), "device-user1");
            assertThat(syncStatus).isPresent();
            assertThat(syncStatus.get().getLastSyncedMsgId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            SyncController.SyncAckRequest request = new SyncController.SyncAckRequest(null, null, true);

            mockMvc.perform(post("/sync/ack")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /sync/status")
    class GetSyncStatusTests {

        @Test
        @DisplayName("Should return sync status with pending messages")
        void shouldReturnSyncStatusWithPendingMessages() throws Exception {
            // Given
            Message msg = createMessage(conversation, user2, "Test message");
            createOfflineMessage(user1, "device-user1", msg);

            // Create sync status
            DeviceSyncStatus syncStatus = DeviceSyncStatus.builder()
                    .user(user1)
                    .deviceId("device-user1")
                    .lastSyncedMsgId(50L)
                    .lastSyncedAt(LocalDateTime.now().minusHours(1))
                    .build();
            deviceSyncStatusRepository.save(syncStatus);

            // When/Then
            mockMvc.perform(get("/sync/status")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.lastSyncedMessageId").value(50))
                    .andExpect(jsonPath("$.data.pendingCount").value(1))
                    .andExpect(jsonPath("$.data.hasPendingMessages").value(true));
        }

        @Test
        @DisplayName("Should return sync status without previous sync")
        void shouldReturnSyncStatusWithoutPreviousSync() throws Exception {
            // When/Then
            mockMvc.perform(get("/sync/status")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.lastSyncedMessageId").isEmpty())
                    .andExpect(jsonPath("$.data.pendingCount").value(0))
                    .andExpect(jsonPath("$.data.hasPendingMessages").value(false));
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(get("/sync/status"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /sync/queue")
    class QueueMessageTests {

        @Test
        @DisplayName("Should queue message for offline user")
        void shouldQueueMessageForOfflineUser() throws Exception {
            // Given
            Message msg = createMessage(conversation, user2, "Test message");

            SyncController.QueueMessageRequest request = new SyncController.QueueMessageRequest(
                    user1.getId(),
                    "device-user1",
                    msg.getId(),
                    conversation.getId()
            );

            // When/Then - Use internal service header (endpoint is for IM server)
            mockMvc.perform(post("/sync/queue")
                            .header("X-Internal-Service", "im-server")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify message was queued
            long pending = offlineMessageRepository.countPendingByUserId(user1.getId());
            assertThat(pending).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not duplicate queue entries")
        void shouldNotDuplicateQueueEntries() throws Exception {
            // Given
            Message msg = createMessage(conversation, user2, "Test message");
            createOfflineMessage(user1, "device-user1", msg);

            SyncController.QueueMessageRequest request = new SyncController.QueueMessageRequest(
                    user1.getId(),
                    "device-user1",
                    msg.getId(),
                    conversation.getId()
            );

            // When - Queue same message again (use internal service header)
            mockMvc.perform(post("/sync/queue")
                            .header("X-Internal-Service", "im-server")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then - Should still have only 1 pending message
            long pending = offlineMessageRepository.countPendingByUserId(user1.getId());
            assertThat(pending).isEqualTo(1);
        }
    }
}
