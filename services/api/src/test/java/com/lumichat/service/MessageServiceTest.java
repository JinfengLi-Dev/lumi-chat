package com.lumichat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.SendMessageRequest;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.entity.Conversation;
import com.lumichat.entity.Message;
import com.lumichat.entity.User;
import com.lumichat.entity.UserConversation;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.ForbiddenException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.repository.MessageRepository;
import com.lumichat.repository.UserConversationRepository;
import com.lumichat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Tests")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserConversationRepository userConversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private User otherUser;
    private Conversation conversation;
    private UserConversation userConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .nickname("TestUser")
                .status(User.UserStatus.active)
                .build();

        otherUser = User.builder()
                .id(2L)
                .uid("LC87654321")
                .email("other@example.com")
                .nickname("OtherUser")
                .status(User.UserStatus.active)
                .build();

        conversation = Conversation.builder()
                .id(100L)
                .type(Conversation.ConversationType.private_chat)
                .participantIds(new Long[]{1L, 2L})
                .createdAt(LocalDateTime.now())
                .build();

        userConversation = UserConversation.builder()
                .id(1L)
                .user(testUser)
                .conversation(conversation)
                .unreadCount(0)
                .build();

        testMessage = Message.builder()
                .id(500L)
                .msgId("msg-123456")
                .conversation(conversation)
                .sender(testUser)
                .senderDeviceId("device-123")
                .msgType(Message.MessageType.text)
                .content("Hello World")
                .serverCreatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GetMessages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Should get messages successfully")
        void shouldGetMessagesSuccessfully() {
            // Given
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(messageRepository.findByConversationIdAfterClearedAt(eq(100L), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList(testMessage)));

            // When
            List<MessageResponse> results = messageService.getMessages(1L, 100L, null, 20);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMsgId()).isEqualTo("msg-123456");
        }

        @Test
        @DisplayName("Should get messages before specific ID")
        void shouldGetMessagesBeforeSpecificId() {
            // Given
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(messageRepository.findBeforeIdAfterClearedAt(eq(100L), eq(500L), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            List<MessageResponse> results = messageService.getMessages(1L, 100L, 500L, 20);

            // Then
            assertThat(results).isEmpty();
            verify(messageRepository).findBeforeIdAfterClearedAt(eq(100L), eq(500L), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should throw exception when conversation not found")
        void shouldThrowExceptionWhenConversationNotFound() {
            // Given
            when(userConversationRepository.findByUserIdAndConversationId(1L, 999L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> messageService.getMessages(1L, 999L, null, 20))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Conversation not found");
        }
    }

    @Nested
    @DisplayName("SendMessage Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send text message successfully")
        void shouldSendTextMessageSuccessfully() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello World");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // When
            MessageResponse result = messageService.sendMessage(1L, "device-123", request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello World");

            verify(messageRepository).save(any(Message.class));
            verify(conversationRepository).save(any(Conversation.class));
            verify(userConversationRepository).incrementUnreadForOthers(100L, 1L);
        }

        @Test
        @DisplayName("Should send message with metadata")
        void shouldSendMessageWithMetadata() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("image");
            request.setContent("image-url");
            request.setMetadata("{\"width\":100,\"height\":100}");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // When
            MessageResponse result = messageService.sendMessage(1L, "device-123", request);

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getMsgType()).isEqualTo(Message.MessageType.image);
            assertThat(savedMessage.getMetadata()).isEqualTo("{\"width\":100,\"height\":100}");
        }

        @Test
        @DisplayName("Should send message with quote")
        void shouldSendMessageWithQuote() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Reply to message");
            request.setQuoteMsgId("quote-msg-123");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getQuoteMsgId()).isEqualTo("quote-msg-123");
        }

        @Test
        @DisplayName("Should send message with client created time")
        void shouldSendMessageWithClientCreatedTime() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello");
            request.setClientCreatedAt("2024-01-15T10:30:00");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getClientCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle invalid client created time gracefully")
        void shouldHandleInvalidClientCreatedTimeGracefully() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello");
            request.setClientCreatedAt("invalid-date");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getClientCreatedAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception for invalid message type")
        void shouldThrowExceptionForInvalidMessageType() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("invalid_type");
            request.setContent("Hello");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> messageService.sendMessage(1L, "device-123", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid message type");
        }

        @Test
        @DisplayName("Should throw exception when conversation not found for send")
        void shouldThrowExceptionWhenConversationNotFoundForSend() {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(999L);
            request.setMsgType("text");
            request.setContent("Hello");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 999L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> messageService.sendMessage(1L, "device-123", request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Conversation not found");
        }
    }

    @Nested
    @DisplayName("RecallMessage Tests")
    class RecallMessageTests {

        @Test
        @DisplayName("Should recall message successfully")
        void shouldRecallMessageSuccessfully() {
            // Given
            testMessage.setServerCreatedAt(LocalDateTime.now()); // Recent message
            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            messageService.recallMessage(1L, "msg-123456");

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getMsgType()).isEqualTo(Message.MessageType.recall);
            assertThat(savedMessage.getRecalledAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when message not found")
        void shouldThrowExceptionWhenMessageNotFound() {
            // Given
            when(messageRepository.findByMsgId("msg-not-found")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> messageService.recallMessage(1L, "msg-not-found"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Message not found");
        }

        @Test
        @DisplayName("Should throw exception when recalling other users message")
        void shouldThrowExceptionWhenRecallingOthersMessage() {
            // Given
            testMessage.setSender(otherUser);
            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));

            // When/Then
            assertThatThrownBy(() -> messageService.recallMessage(1L, "msg-123456"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Cannot recall message sent by another user");
        }

        @Test
        @DisplayName("Should throw exception when recall time limit exceeded")
        void shouldThrowExceptionWhenRecallTimeLimitExceeded() {
            // Given
            testMessage.setServerCreatedAt(LocalDateTime.now().minusMinutes(5)); // 5 minutes ago
            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));

            // When/Then
            assertThatThrownBy(() -> messageService.recallMessage(1L, "msg-123456"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot recall message after 2 minutes");
        }
    }

    @Nested
    @DisplayName("ForwardMessage Tests")
    class ForwardMessageTests {

        @Test
        @DisplayName("Should forward message successfully")
        void shouldForwardMessageSuccessfully() {
            // Given
            Conversation targetConversation = Conversation.builder()
                    .id(200L)
                    .type(Conversation.ConversationType.private_chat)
                    .build();

            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 200L))
                    .thenReturn(Optional.of(UserConversation.builder()
                            .user(testUser)
                            .conversation(targetConversation)
                            .build()));
            when(conversationRepository.findById(200L)).thenReturn(Optional.of(targetConversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(502L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(targetConversation);

            // When
            MessageResponse result = messageService.forwardMessage(1L, "device-123", "msg-123456", 200L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello World");

            verify(messageRepository).save(any(Message.class));
            verify(userConversationRepository).incrementUnreadForOthers(200L, 1L);
        }

        @Test
        @DisplayName("Should throw exception when original message not found")
        void shouldThrowExceptionWhenOriginalMessageNotFound() {
            // Given
            when(messageRepository.findByMsgId("msg-not-found")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> messageService.forwardMessage(1L, "device-123", "msg-not-found", 200L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Message not found");
        }

        @Test
        @DisplayName("Should throw exception when target conversation not accessible")
        void shouldThrowExceptionWhenTargetConversationNotAccessible() {
            // Given
            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 200L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> messageService.forwardMessage(1L, "device-123", "msg-123456", 200L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Target conversation not found");
        }
    }

    @Nested
    @DisplayName("DeleteMessage Tests")
    class DeleteMessageTests {

        @Test
        @DisplayName("Should delete message successfully")
        void shouldDeleteMessageSuccessfully() {
            // Given
            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));

            // When
            messageService.deleteMessage(1L, "msg-123456");

            // Then
            verify(messageRepository).delete(testMessage);
        }

        @Test
        @DisplayName("Should throw exception when deleting others message")
        void shouldThrowExceptionWhenDeletingOthersMessage() {
            // Given
            testMessage.setSender(otherUser);
            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));

            // When/Then
            assertThatThrownBy(() -> messageService.deleteMessage(1L, "msg-123456"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can only delete your own messages");
        }

        @Test
        @DisplayName("Should throw exception when message not found for delete")
        void shouldThrowExceptionWhenMessageNotFoundForDelete() {
            // Given
            when(messageRepository.findByMsgId("msg-not-found")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> messageService.deleteMessage(1L, "msg-not-found"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Message not found");
        }
    }

    @Nested
    @DisplayName("Redis Publishing Tests")
    class RedisPublishingTests {

        @Test
        @DisplayName("Should publish message to Redis after saving")
        void shouldPublishToRedisAfterSaving() throws JsonProcessingException {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello World");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then
            verify(redisTemplate).convertAndSend(eq("im:messages"), anyString());
        }

        @Test
        @DisplayName("Should include correct message format in Redis payload")
        void shouldIncludeCorrectMessageFormat() throws JsonProcessingException {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Test content");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // Capture the map passed to objectMapper
            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(mapCaptor.capture())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            assertThat(capturedEvent).containsKeys("type", "senderId", "senderDeviceId", "conversationId", "msgId", "message");
            assertThat(capturedEvent.get("type")).isEqualTo("chat_message");
            assertThat(capturedEvent.get("senderId")).isEqualTo(1L);
            assertThat(capturedEvent.get("senderDeviceId")).isEqualTo("device-123");
            assertThat(capturedEvent.get("conversationId")).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should continue saving message even if Redis publish fails")
        void shouldContinueWhenRedisPublishFails() throws JsonProcessingException {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // Make Redis throw an exception
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Redis connection failed"));

            // When
            MessageResponse result = messageService.sendMessage(1L, "device-123", request);

            // Then - message should still be saved and returned
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello");
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("Should include sender info in Redis message payload")
        void shouldIncludeSenderInfoInRedisMessage() throws JsonProcessingException {
            // Given
            testUser.setAvatar("https://example.com/avatar.jpg");
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(mapCaptor.capture())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> messagePayload = (Map<String, Object>) capturedEvent.get("message");
            @SuppressWarnings("unchecked")
            Map<String, Object> sender = (Map<String, Object>) messagePayload.get("sender");

            assertThat(sender).containsKeys("id", "nickname", "avatar");
            assertThat(sender.get("id")).isEqualTo(1L);
            assertThat(sender.get("nickname")).isEqualTo("TestUser");
            assertThat(sender.get("avatar")).isEqualTo("https://example.com/avatar.jpg");
        }

        @Test
        @DisplayName("Should use DB message ID (Long) as msgId for offline queue compatibility")
        void shouldUseDbMessageIdForOfflineQueue() throws JsonProcessingException {
            // Given
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L); // This is the DB ID that should be used
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(mapCaptor.capture())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then - the outer msgId should be the DB ID as string (parseable as Long)
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            String msgId = (String) capturedEvent.get("msgId");
            assertThat(msgId).isEqualTo("501"); // DB ID as string
            assertThat(Long.parseLong(msgId)).isEqualTo(501L); // Should be parseable as Long
        }

        @Test
        @DisplayName("Should parse metadata JSON to Object to avoid double-encoding")
        void shouldParseMetadataJsonToObject() throws JsonProcessingException {
            // Given - image message with metadata containing fileUrl
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("image");
            request.setContent("/api/v1/files/abc123");
            request.setMetadata("{\"fileUrl\":\"/api/v1/files/abc123\",\"thumbnailUrl\":\"/api/v1/files/thumb123\",\"width\":800,\"height\":600}");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // Mock objectMapper.readValue to parse the metadata JSON string to a Map
            ObjectMapper realMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedMetadata = realMapper.readValue(
                    "{\"fileUrl\":\"/api/v1/files/abc123\",\"thumbnailUrl\":\"/api/v1/files/thumb123\",\"width\":800,\"height\":600}",
                    Map.class
            );
            when(objectMapper.readValue(anyString(), eq(Object.class))).thenReturn(parsedMetadata);

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(mapCaptor.capture())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then - verify metadata in the message payload is an Object (Map), not a String
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> messagePayload = (Map<String, Object>) capturedEvent.get("message");
            Object metadata = messagePayload.get("metadata");

            // The key assertion: metadata should be a Map (parsed JSON), not a String
            assertThat(metadata).isNotNull();
            assertThat(metadata).isInstanceOf(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = (Map<String, Object>) metadata;
            assertThat(metadataMap.get("fileUrl")).isEqualTo("/api/v1/files/abc123");
            assertThat(metadataMap.get("thumbnailUrl")).isEqualTo("/api/v1/files/thumb123");
            assertThat(metadataMap.get("width")).isEqualTo(800);
            assertThat(metadataMap.get("height")).isEqualTo(600);
        }

        @Test
        @DisplayName("Should handle null metadata gracefully")
        void shouldHandleNullMetadataGracefully() throws JsonProcessingException {
            // Given - text message without metadata
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("text");
            request.setContent("Hello World");
            request.setMetadata(null);

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(mapCaptor.capture())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then - metadata should be null, not empty string or throw exception
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> messagePayload = (Map<String, Object>) capturedEvent.get("message");

            assertThat(messagePayload.get("metadata")).isNull();
            // Verify objectMapper.readValue was never called for null metadata
            verify(objectMapper, never()).readValue((String) isNull(), eq(Object.class));
        }

        @Test
        @DisplayName("Should fallback to string when metadata parsing fails")
        void shouldFallbackToStringWhenMetadataParsingFails() throws JsonProcessingException {
            // Given - message with invalid JSON in metadata field
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(100L);
            request.setMsgType("image");
            request.setContent("/api/v1/files/abc123");
            request.setMetadata("invalid-json{");

            when(userConversationRepository.findByUserIdAndConversationId(1L, 100L))
                    .thenReturn(Optional.of(userConversation));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(501L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            // Mock objectMapper.readValue to throw exception for invalid JSON
            when(objectMapper.readValue(eq("invalid-json{"), eq(Object.class)))
                    .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(mapCaptor.capture())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.sendMessage(1L, "device-123", request);

            // Then - metadata should fallback to original string value
            Map<String, Object> capturedEvent = mapCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> messagePayload = (Map<String, Object>) capturedEvent.get("message");

            // Should fallback to the original string when parsing fails
            assertThat(messagePayload.get("metadata")).isEqualTo("invalid-json{");
        }

        @Test
        @DisplayName("Should publish to Redis when forwarding message")
        void shouldPublishToRedisWhenForwarding() throws JsonProcessingException {
            // Given
            Conversation targetConversation = Conversation.builder()
                    .id(200L)
                    .type(Conversation.ConversationType.private_chat)
                    .build();

            when(messageRepository.findByMsgId("msg-123456")).thenReturn(Optional.of(testMessage));
            when(userConversationRepository.findByUserIdAndConversationId(1L, 200L))
                    .thenReturn(Optional.of(UserConversation.builder()
                            .user(testUser)
                            .conversation(targetConversation)
                            .build()));
            when(conversationRepository.findById(200L)).thenReturn(Optional.of(targetConversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(502L);
                m.setServerCreatedAt(LocalDateTime.now());
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(targetConversation);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");

            // When
            messageService.forwardMessage(1L, "device-123", "msg-123456", 200L);

            // Then
            verify(redisTemplate).convertAndSend(eq("im:messages"), anyString());
        }
    }
}
