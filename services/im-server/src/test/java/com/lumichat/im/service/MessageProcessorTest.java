package com.lumichat.im.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.client.ApiClient;
import com.lumichat.im.protocol.Packet;
import com.lumichat.im.protocol.ProtocolType;
import com.lumichat.im.security.JwtTokenValidator;
import com.lumichat.im.session.SessionManager;
import com.lumichat.im.session.UserSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageProcessor Tests")
class MessageProcessorTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtTokenValidator jwtTokenValidator;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Channel channel;

    @Mock
    private ChannelId channelId;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private SetOperations<String, String> setOperations;

    private ObjectMapper objectMapper;
    private MessageProcessor messageProcessor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageProcessor = new MessageProcessor(
                sessionManager, objectMapper, redisTemplate, jwtTokenValidator, apiClient);

        // Common channel mock setup
        lenient().when(ctx.channel()).thenReturn(channel);
        lenient().when(channel.id()).thenReturn(channelId);
        lenient().when(channelId.asLongText()).thenReturn("test-channel-id");
        lenient().when(ctx.writeAndFlush(any())).thenReturn(channelFuture);
        lenient().when(channel.writeAndFlush(any())).thenReturn(channelFuture);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Nested
    @DisplayName("Login Handler Tests")
    class LoginHandlerTests {

        @Test
        @DisplayName("Should login successfully with valid JWT token")
        void shouldLoginSuccessfullyWithValidToken() {
            // Given
            String validToken = "valid-jwt-token";
            String deviceId = "device-123";
            Long userId = 1L;

            Packet loginPacket = Packet.builder()
                    .type(ProtocolType.LOGIN)
                    .seq("seq-1")
                    .data(Map.of("token", validToken, "deviceId", deviceId, "deviceType", "web"))
                    .build();

            when(jwtTokenValidator.validateToken(validToken))
                    .thenReturn(new JwtTokenValidator.TokenInfo(userId, deviceId));
            when(sessionManager.getSessionsByUserId(userId))
                    .thenReturn(Collections.emptyList());

            // When
            messageProcessor.handleLogin(ctx, loginPacket);

            // Then
            verify(sessionManager).addSession(channel, userId, deviceId, "web");
            verify(setOperations).add("online:users", userId.toString());

            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
            assertThat(responseJson).contains("\"userId\":" + userId);
        }

        @Test
        @DisplayName("Should reject login with invalid token")
        void shouldRejectLoginWithInvalidToken() {
            // Given
            String invalidToken = "invalid-jwt-token";
            Packet loginPacket = Packet.builder()
                    .type(ProtocolType.LOGIN)
                    .seq("seq-1")
                    .data(Map.of("token", invalidToken, "deviceId", "device-123", "deviceType", "web"))
                    .build();

            when(jwtTokenValidator.validateToken(invalidToken)).thenReturn(null);

            // When
            messageProcessor.handleLogin(ctx, loginPacket);

            // Then
            verify(sessionManager, never()).addSession(any(), any(), any(), any());

            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":false");
            assertThat(responseJson).contains("Invalid or expired token");
        }

        @Test
        @DisplayName("Should reject login when device ID mismatches token")
        void shouldRejectLoginWhenDeviceIdMismatches() {
            // Given
            String validToken = "valid-jwt-token";
            String tokenDeviceId = "device-in-token";
            String requestDeviceId = "different-device";
            Long userId = 1L;

            Packet loginPacket = Packet.builder()
                    .type(ProtocolType.LOGIN)
                    .seq("seq-1")
                    .data(Map.of("token", validToken, "deviceId", requestDeviceId, "deviceType", "web"))
                    .build();

            when(jwtTokenValidator.validateToken(validToken))
                    .thenReturn(new JwtTokenValidator.TokenInfo(userId, tokenDeviceId));

            // When
            messageProcessor.handleLogin(ctx, loginPacket);

            // Then
            verify(sessionManager, never()).addSession(any(), any(), any(), any());

            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":false");
            assertThat(responseJson).contains("Device ID mismatch");
        }

        @Test
        @DisplayName("Should broadcast online status for first device login")
        void shouldBroadcastOnlineStatusForFirstDevice() {
            // Given
            String validToken = "valid-jwt-token";
            String deviceId = "device-123";
            Long userId = 1L;

            Packet loginPacket = Packet.builder()
                    .type(ProtocolType.LOGIN)
                    .seq("seq-1")
                    .data(Map.of("token", validToken, "deviceId", deviceId, "deviceType", "web"))
                    .build();

            when(jwtTokenValidator.validateToken(validToken))
                    .thenReturn(new JwtTokenValidator.TokenInfo(userId, deviceId));
            when(sessionManager.getSessionsByUserId(userId))
                    .thenReturn(Collections.emptyList()); // No existing sessions = first device
            when(sessionManager.getAllSessions())
                    .thenReturn(Collections.emptyList());

            // When
            messageProcessor.handleLogin(ctx, loginPacket);

            // Then
            verify(sessionManager).addSession(channel, userId, deviceId, "web");
            // Online status broadcast called because it's first device
            verify(sessionManager).getAllSessions();
        }

        @Test
        @DisplayName("Should not broadcast online status for second device login")
        void shouldNotBroadcastOnlineStatusForSecondDevice() {
            // Given
            String validToken = "valid-jwt-token";
            String deviceId = "device-123";
            Long userId = 1L;

            UserSession existingSession = mock(UserSession.class);

            Packet loginPacket = Packet.builder()
                    .type(ProtocolType.LOGIN)
                    .seq("seq-1")
                    .data(Map.of("token", validToken, "deviceId", deviceId, "deviceType", "web"))
                    .build();

            when(jwtTokenValidator.validateToken(validToken))
                    .thenReturn(new JwtTokenValidator.TokenInfo(userId, deviceId));
            when(sessionManager.getSessionsByUserId(userId))
                    .thenReturn(List.of(existingSession)); // Has existing session = not first device

            // When
            messageProcessor.handleLogin(ctx, loginPacket);

            // Then
            verify(sessionManager).addSession(channel, userId, deviceId, "web");
            // getAllSessions should not be called for status broadcast
            verify(sessionManager, never()).getAllSessions();
        }
    }

    @Nested
    @DisplayName("Chat Message Handler Tests")
    class ChatMessageHandlerTests {

        private UserSession senderSession;

        @BeforeEach
        void setUp() {
            senderSession = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();
        }

        @Test
        @DisplayName("Should process chat message and send ACK")
        void shouldProcessChatMessageAndSendAck() {
            // Given
            when(sessionManager.getSessionByChannel(channel)).thenReturn(senderSession);
            when(apiClient.persistMessage(eq(1L), eq("device-123"), any()))
                    .thenReturn(new ApiClient.MessagePersistResult(
                            true, "server-msg-id", "2024-01-01T00:00:00Z", null));

            Packet chatPacket = Packet.builder()
                    .type(ProtocolType.CHAT_MESSAGE)
                    .seq("seq-1")
                    .data(Map.of(
                            "conversationId", 100,
                            "msgId", "client-msg-id",
                            "msgType", "text",
                            "content", "Hello, World!"
                    ))
                    .build();

            // When
            messageProcessor.handleChatMessage(ctx, chatPacket);

            // Then
            verify(apiClient).persistMessage(eq(1L), eq("device-123"), any());

            // Verify ACK sent
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
            assertThat(responseJson).contains("\"msgId\":\"server-msg-id\"");
            assertThat(responseJson).contains("\"clientMsgId\":\"client-msg-id\"");

            // Verify message published to Redis
            verify(redisTemplate).convertAndSend(eq("im:messages"), anyString());
        }

        @Test
        @DisplayName("Should reject message from unauthenticated channel")
        void shouldRejectMessageFromUnauthenticatedChannel() {
            // Given
            when(sessionManager.getSessionByChannel(channel)).thenReturn(null);

            Packet chatPacket = Packet.builder()
                    .type(ProtocolType.CHAT_MESSAGE)
                    .seq("seq-1")
                    .data(Map.of(
                            "conversationId", 100,
                            "msgId", "client-msg-id",
                            "msgType", "text",
                            "content", "Hello"
                    ))
                    .build();

            // When
            messageProcessor.handleChatMessage(ctx, chatPacket);

            // Then
            verify(apiClient, never()).persistMessage(any(), any(), any());
            verify(ctx, never()).writeAndFlush(any());
        }

        @Test
        @DisplayName("Should handle API persist failure")
        void shouldHandleApiPersistFailure() {
            // Given
            when(sessionManager.getSessionByChannel(channel)).thenReturn(senderSession);
            when(apiClient.persistMessage(eq(1L), eq("device-123"), any()))
                    .thenReturn(new ApiClient.MessagePersistResult(
                            false, null, null, "Database error"));

            Packet chatPacket = Packet.builder()
                    .type(ProtocolType.CHAT_MESSAGE)
                    .seq("seq-1")
                    .data(Map.of(
                            "conversationId", 100,
                            "msgId", "client-msg-id",
                            "msgType", "text",
                            "content", "Hello"
                    ))
                    .build();

            // When
            messageProcessor.handleChatMessage(ctx, chatPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":false");
            assertThat(responseJson).contains("Database error");

            // Should not publish to Redis on failure
            verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Heartbeat Handler Tests")
    class HeartbeatHandlerTests {

        @Test
        @DisplayName("Should respond to heartbeat with server time")
        void shouldRespondToHeartbeatWithServerTime() {
            // Given
            Packet heartbeatPacket = Packet.builder()
                    .type(ProtocolType.HEARTBEAT)
                    .seq("seq-1")
                    .build();

            // When
            messageProcessor.handleHeartbeat(ctx, heartbeatPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"type\":" + ProtocolType.HEARTBEAT_RESPONSE);
            assertThat(responseJson).contains("serverTime");
        }
    }

    @Nested
    @DisplayName("Typing Handler Tests")
    class TypingHandlerTests {

        @Test
        @DisplayName("Should publish typing notification to Redis")
        void shouldPublishTypingNotificationToRedis() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);

            Packet typingPacket = Packet.builder()
                    .type(ProtocolType.TYPING)
                    .seq("seq-1")
                    .data(Map.of("conversationId", 100))
                    .build();

            // When
            messageProcessor.handleTyping(ctx, typingPacket);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).convertAndSend(eq("im:typing"), messageCaptor.capture());

            String publishedMessage = messageCaptor.getValue();
            assertThat(publishedMessage).contains("\"type\":\"typing\"");
            assertThat(publishedMessage).contains("\"userId\":1");
            assertThat(publishedMessage).contains("\"conversationId\":100");
        }

        @Test
        @DisplayName("Should ignore typing from unauthenticated channel")
        void shouldIgnoreTypingFromUnauthenticatedChannel() {
            // Given
            when(sessionManager.getSessionByChannel(channel)).thenReturn(null);

            Packet typingPacket = Packet.builder()
                    .type(ProtocolType.TYPING)
                    .seq("seq-1")
                    .data(Map.of("conversationId", 100))
                    .build();

            // When
            messageProcessor.handleTyping(ctx, typingPacket);

            // Then
            verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Read Acknowledgment Handler Tests")
    class ReadAckHandlerTests {

        @Test
        @DisplayName("Should update read status and notify sender")
        void shouldUpdateReadStatusAndNotifySender() {
            // Given
            UserSession readerSession = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            UserSession senderSession = mock(UserSession.class);
            Channel senderChannel = mock(Channel.class);
            when(senderSession.getChannel()).thenReturn(senderChannel);
            when(senderChannel.writeAndFlush(any())).thenReturn(channelFuture);

            when(sessionManager.getSessionByChannel(channel)).thenReturn(readerSession);
            when(apiClient.updateReadStatus(1L, 100L, 50L))
                    .thenReturn(new ApiClient.ReadStatusResult(
                            true, 100L, 50L, 1L, 2L, null)); // notifyUserId = 2
            when(sessionManager.getSessionsByUserId(2L))
                    .thenReturn(List.of(senderSession));

            Packet readAckPacket = Packet.builder()
                    .type(ProtocolType.READ_ACK)
                    .seq("seq-1")
                    .data(Map.of("conversationId", 100, "lastReadMsgId", 50))
                    .build();

            // When
            messageProcessor.handleReadAck(ctx, readAckPacket);

            // Then
            verify(apiClient).updateReadStatus(1L, 100L, 50L);
            verify(redisTemplate).convertAndSend(eq("im:read_status"), anyString());

            // Verify notification sent to sender
            verify(senderChannel).writeAndFlush(any(TextWebSocketFrame.class));
        }

        @Test
        @DisplayName("Should ignore read ack from unauthenticated channel")
        void shouldIgnoreReadAckFromUnauthenticatedChannel() {
            // Given
            when(sessionManager.getSessionByChannel(channel)).thenReturn(null);

            Packet readAckPacket = Packet.builder()
                    .type(ProtocolType.READ_ACK)
                    .seq("seq-1")
                    .data(Map.of("conversationId", 100, "lastReadMsgId", 50))
                    .build();

            // When
            messageProcessor.handleReadAck(ctx, readAckPacket);

            // Then
            verify(apiClient, never()).updateReadStatus(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Recall Handler Tests")
    class RecallHandlerTests {

        @Test
        @DisplayName("Should recall message and broadcast")
        void shouldRecallMessageAndBroadcast() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);
            when(apiClient.recallMessage(1L, "msg-123"))
                    .thenReturn(new ApiClient.RecallResult(true, null));

            Packet recallPacket = Packet.builder()
                    .type(ProtocolType.RECALL_MESSAGE)
                    .seq("seq-1")
                    .data(Map.of("msgId", "msg-123", "conversationId", 100))
                    .build();

            // When
            messageProcessor.handleRecall(ctx, recallPacket);

            // Then
            verify(apiClient).recallMessage(1L, "msg-123");

            // Verify ACK sent
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
            assertThat(responseJson).contains("\"msgId\":\"msg-123\"");

            // Verify recall published to Redis
            verify(redisTemplate).convertAndSend(eq("im:recall"), anyString());
        }

        @Test
        @DisplayName("Should handle recall failure")
        void shouldHandleRecallFailure() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);
            when(apiClient.recallMessage(1L, "msg-123"))
                    .thenReturn(new ApiClient.RecallResult(false, "Time limit exceeded"));

            Packet recallPacket = Packet.builder()
                    .type(ProtocolType.RECALL_MESSAGE)
                    .seq("seq-1")
                    .data(Map.of("msgId", "msg-123", "conversationId", 100))
                    .build();

            // When
            messageProcessor.handleRecall(ctx, recallPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":false");
            assertThat(responseJson).contains("Time limit exceeded");

            // Should not publish to Redis on failure
            verify(redisTemplate, never()).convertAndSend(eq("im:recall"), anyString());
        }
    }

    @Nested
    @DisplayName("Sync Request Handler Tests")
    class SyncRequestHandlerTests {

        @Test
        @DisplayName("Should fetch and return messages for sync")
        void shouldFetchAndReturnMessagesForSync() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            List<Map<String, Object>> messages = List.of(
                    Map.of("msgId", "msg-1", "content", "Hello"),
                    Map.of("msgId", "msg-2", "content", "World")
            );

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);
            when(apiClient.getMessagesForSync(1L, 100L, 50L, 50))
                    .thenReturn(messages);

            Packet syncPacket = Packet.builder()
                    .type(ProtocolType.SYNC_REQUEST)
                    .seq("seq-1")
                    .data(Map.of("conversationId", 100, "afterMsgId", 50, "limit", 50))
                    .build();

            // When
            messageProcessor.handleSyncRequest(ctx, syncPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
            assertThat(responseJson).contains("\"conversationId\":100");
            assertThat(responseJson).contains("messages");
        }

        @Test
        @DisplayName("Should reject sync without conversationId")
        void shouldRejectSyncWithoutConversationId() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);

            Packet syncPacket = Packet.builder()
                    .type(ProtocolType.SYNC_REQUEST)
                    .seq("seq-1")
                    .data(Map.of("afterMsgId", 50))
                    .build();

            // When
            messageProcessor.handleSyncRequest(ctx, syncPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":false");
            assertThat(responseJson).contains("conversationId is required");
        }
    }

    @Nested
    @DisplayName("Logout Handler Tests")
    class LogoutHandlerTests {

        @Test
        @DisplayName("Should logout and close channel")
        void shouldLogoutAndCloseChannel() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);
            when(sessionManager.getSessionsByUserId(1L)).thenReturn(Collections.emptyList());
            when(sessionManager.getAllSessions()).thenReturn(Collections.emptyList());

            Packet logoutPacket = Packet.builder()
                    .type(ProtocolType.LOGOUT)
                    .seq("seq-1")
                    .build();

            // When
            messageProcessor.handleLogout(ctx, logoutPacket);

            // Then
            verify(setOperations).remove("online:users", "1");
            verify(ctx).close();

            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
        }
    }

    @Nested
    @DisplayName("Send To User Tests")
    class SendToUserTests {

        @Test
        @DisplayName("Should send packet to all user sessions")
        void shouldSendPacketToAllUserSessions() {
            // Given
            UserSession session1 = mock(UserSession.class);
            UserSession session2 = mock(UserSession.class);
            Channel channel1 = mock(Channel.class);
            Channel channel2 = mock(Channel.class);

            when(session1.getChannel()).thenReturn(channel1);
            when(session2.getChannel()).thenReturn(channel2);
            when(channel1.writeAndFlush(any())).thenReturn(channelFuture);
            when(channel2.writeAndFlush(any())).thenReturn(channelFuture);

            when(sessionManager.getSessionsByUserId(1L))
                    .thenReturn(List.of(session1, session2));

            Packet packet = Packet.of(ProtocolType.RECEIVE_MESSAGE, Map.of("msgId", "msg-1"));

            // When
            messageProcessor.sendToUser(1L, packet);

            // Then
            verify(channel1).writeAndFlush(any(TextWebSocketFrame.class));
            verify(channel2).writeAndFlush(any(TextWebSocketFrame.class));
        }

        @Test
        @DisplayName("Should handle no active sessions gracefully")
        void shouldHandleNoActiveSessionsGracefully() {
            // Given
            when(sessionManager.getSessionsByUserId(1L)).thenReturn(Collections.emptyList());

            Packet packet = Packet.of(ProtocolType.RECEIVE_MESSAGE, Map.of("msgId", "msg-1"));

            // When - Should not throw
            messageProcessor.sendToUser(1L, packet);

            // Then - Nothing to verify, just ensuring no exception
        }
    }

    @Nested
    @DisplayName("Disconnect Handler Tests")
    class DisconnectHandlerTests {

        @Test
        @DisplayName("Should remove user from online set when last device disconnects")
        void shouldRemoveFromOnlineSetWhenLastDeviceDisconnects() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionsByUserId(1L))
                    .thenReturn(List.of(session)); // Only this session
            when(sessionManager.getAllSessions()).thenReturn(Collections.emptyList());

            // When
            messageProcessor.handleDisconnect(session);

            // Then
            verify(setOperations).remove("online:users", "1");
        }

        @Test
        @DisplayName("Should not remove from online set when other devices are connected")
        void shouldNotRemoveFromOnlineSetWhenOtherDevicesConnected() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            UserSession otherSession = mock(UserSession.class);

            when(sessionManager.getSessionsByUserId(1L))
                    .thenReturn(List.of(session, otherSession)); // Two sessions

            // When
            messageProcessor.handleDisconnect(session);

            // Then
            verify(setOperations, never()).remove(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Online Status Handler Tests")
    class OnlineStatusHandlerTests {

        @Test
        @DisplayName("Should return online statuses for requested users")
        void shouldReturnOnlineStatusesForRequestedUsers() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);
            when(setOperations.isMember("online:users", "2")).thenReturn(true);
            when(setOperations.isMember("online:users", "3")).thenReturn(false);

            Packet statusPacket = Packet.builder()
                    .type(ProtocolType.ONLINE_STATUS_REQUEST)
                    .seq("seq-1")
                    .data(Map.of("userIds", List.of(2, 3)))
                    .build();

            // When
            messageProcessor.handleOnlineStatusRequest(ctx, statusPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
            assertThat(responseJson).contains("statuses");
        }

        @Test
        @DisplayName("Should handle empty user list")
        void shouldHandleEmptyUserList() {
            // Given
            UserSession session = UserSession.builder()
                    .userId(1L)
                    .deviceId("device-123")
                    .channel(channel)
                    .build();

            when(sessionManager.getSessionByChannel(channel)).thenReturn(session);

            Packet statusPacket = Packet.builder()
                    .type(ProtocolType.ONLINE_STATUS_REQUEST)
                    .seq("seq-1")
                    .data(Map.of("userIds", List.of()))
                    .build();

            // When
            messageProcessor.handleOnlineStatusRequest(ctx, statusPacket);

            // Then
            ArgumentCaptor<TextWebSocketFrame> frameCaptor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
            verify(ctx).writeAndFlush(frameCaptor.capture());

            String responseJson = frameCaptor.getValue().text();
            assertThat(responseJson).contains("\"success\":true");
        }
    }
}
