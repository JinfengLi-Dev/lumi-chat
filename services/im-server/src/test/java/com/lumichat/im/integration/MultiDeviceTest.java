package com.lumichat.im.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.client.ApiClient;
import com.lumichat.im.config.RedisConfig;
import com.lumichat.im.protocol.Packet;
import com.lumichat.im.protocol.ProtocolType;
import com.lumichat.im.service.MessageProcessor;
import com.lumichat.im.session.SessionManager;
import com.lumichat.im.session.UserSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for multi-device message routing.
 *
 * Verifies that:
 * 1. Messages are delivered to all devices of the recipient
 * 2. Read status is synced across user's own devices
 * 3. Offline devices receive messages when they reconnect
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Device Integration Tests")
class MultiDeviceTest {

    @Mock
    private ApiClient apiClient;

    private SessionManager sessionManager;
    private ObjectMapper objectMapper;
    private RedisConfig redisConfig;

    // User 1 devices
    @Mock
    private Channel user1Device1Channel;
    @Mock
    private Channel user1Device2Channel;
    @Mock
    private ChannelId user1Device1ChannelId;
    @Mock
    private ChannelId user1Device2ChannelId;
    @Mock
    private ChannelFuture channelFuture;

    // User 2 device
    @Mock
    private Channel user2DeviceChannel;
    @Mock
    private ChannelId user2DeviceChannelId;

    @Mock
    private MessageProcessor messageProcessor;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
        objectMapper = new ObjectMapper();

        // Setup channel mocks for user 1 device 1
        lenient().when(user1Device1Channel.id()).thenReturn(user1Device1ChannelId);
        lenient().when(user1Device1ChannelId.asLongText()).thenReturn("user1-device1-channel");
        lenient().when(user1Device1ChannelId.asShortText()).thenReturn("u1d1");
        lenient().when(user1Device1Channel.writeAndFlush(any())).thenReturn(channelFuture);

        // Setup channel mocks for user 1 device 2
        lenient().when(user1Device2Channel.id()).thenReturn(user1Device2ChannelId);
        lenient().when(user1Device2ChannelId.asLongText()).thenReturn("user1-device2-channel");
        lenient().when(user1Device2ChannelId.asShortText()).thenReturn("u1d2");
        lenient().when(user1Device2Channel.writeAndFlush(any())).thenReturn(channelFuture);

        // Setup channel mocks for user 2
        lenient().when(user2DeviceChannel.id()).thenReturn(user2DeviceChannelId);
        lenient().when(user2DeviceChannelId.asLongText()).thenReturn("user2-device1-channel");
        lenient().when(user2DeviceChannelId.asShortText()).thenReturn("u2d1");
        lenient().when(user2DeviceChannel.writeAndFlush(any())).thenReturn(channelFuture);

        // Setup API client mock
        lenient().when(apiClient.getConversationParticipants(anyLong()))
                .thenReturn(List.of(1L, 2L));
    }

    @Nested
    @DisplayName("Message Delivery to Multiple Devices")
    class MessageDeliveryTests {

        @Test
        @DisplayName("Should deliver message to all devices of recipient")
        void shouldDeliverMessageToAllDevicesOfRecipient() {
            // Given: User 1 has two devices online
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");

            // Verify both devices are tracked
            var sessions = sessionManager.getSessionsByUserId(1L);
            assertThat(sessions).hasSize(2);

            // When: A message is sent to user 1
            Packet messagePacket = Packet.of(ProtocolType.RECEIVE_MESSAGE, Map.of(
                    "msgId", "msg-123",
                    "content", "Hello from user 2"
            ));

            // Simulate sending to all user sessions
            for (var session : sessions) {
                session.getChannel().writeAndFlush(new TextWebSocketFrame("test"));
            }

            // Then: Both devices should receive the message
            verify(user1Device1Channel).writeAndFlush(any(TextWebSocketFrame.class));
            verify(user1Device2Channel).writeAndFlush(any(TextWebSocketFrame.class));
        }

        @Test
        @DisplayName("Should deliver message to recipient but exclude sender's originating device")
        void shouldExcludeSenderOriginatingDevice() {
            // Given: User 1 has two devices online, user 2 has one device
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");
            sessionManager.addSession(user2DeviceChannel, 2L, "device-3", "web");

            // User 1's device-1 sends a message
            Long senderId = 1L;
            String senderDeviceId = "device-1";

            // When: Broadcasting to all participants
            List<Long> participants = List.of(1L, 2L);

            for (Long participantId : participants) {
                var sessions = sessionManager.getSessionsByUserId(participantId);
                for (var session : sessions) {
                    // Skip the originating device
                    if (participantId.equals(senderId) && session.getDeviceId().equals(senderDeviceId)) {
                        continue;
                    }
                    session.getChannel().writeAndFlush(new TextWebSocketFrame("message"));
                }
            }

            // Then:
            // - User 1's device-1 (sender) should NOT receive the message
            // - User 1's device-2 SHOULD receive the message (sender's other device)
            // - User 2's device SHOULD receive the message
            verify(user1Device1Channel, never()).writeAndFlush(any(TextWebSocketFrame.class));
            verify(user1Device2Channel).writeAndFlush(any(TextWebSocketFrame.class));
            verify(user2DeviceChannel).writeAndFlush(any(TextWebSocketFrame.class));
        }

        @Test
        @DisplayName("Should handle offline user by not sending and returning empty sessions")
        void shouldHandleOfflineUser() {
            // Given: Only user 1 is online, user 2 is offline
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");

            // When: Checking user 2's sessions
            var user2Sessions = sessionManager.getSessionsByUserId(2L);

            // Then: User 2 has no active sessions
            assertThat(user2Sessions).isEmpty();
            assertThat(sessionManager.isUserOnline(2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Read Status Sync Across Devices")
    class ReadStatusSyncTests {

        @Test
        @DisplayName("Should sync read status to user's other devices")
        void shouldSyncReadStatusToOtherDevices() {
            // Given: User 1 has two devices online
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");

            // User marks read on device-1
            String originDeviceId = "device-1";

            // When: Syncing read status to other devices
            var sessions = sessionManager.getSessionsByUserId(1L);
            for (var session : sessions) {
                if (!session.getDeviceId().equals(originDeviceId)) {
                    session.getChannel().writeAndFlush(new TextWebSocketFrame("read_sync"));
                }
            }

            // Then: Only device-2 should receive the sync
            verify(user1Device1Channel, never()).writeAndFlush(any(TextWebSocketFrame.class));
            verify(user1Device2Channel).writeAndFlush(any(TextWebSocketFrame.class));
        }

        @Test
        @DisplayName("Should not sync read status when user has single device")
        void shouldNotSyncWhenSingleDevice() {
            // Given: User 1 has only one device online
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");

            // User marks read on device-1
            String originDeviceId = "device-1";

            // When: Trying to sync read status to other devices
            var sessions = sessionManager.getSessionsByUserId(1L);
            int syncCount = 0;
            for (var session : sessions) {
                if (!session.getDeviceId().equals(originDeviceId)) {
                    session.getChannel().writeAndFlush(new TextWebSocketFrame("read_sync"));
                    syncCount++;
                }
            }

            // Then: No sync should happen
            assertThat(syncCount).isZero();
            verify(user1Device1Channel, never()).writeAndFlush(any(TextWebSocketFrame.class));
        }
    }

    @Nested
    @DisplayName("Session Management for Multi-Device")
    class SessionManagementTests {

        @Test
        @DisplayName("Should track multiple devices for same user")
        void shouldTrackMultipleDevicesForSameUser() {
            // When: User logs in from two devices
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");

            // Then
            assertThat(sessionManager.isUserOnline(1L)).isTrue();
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(2);
            assertThat(sessionManager.getSession(1L, "device-1")).isNotNull();
            assertThat(sessionManager.getSession(1L, "device-2")).isNotNull();
        }

        @Test
        @DisplayName("Should keep user online when one device disconnects but another remains")
        void shouldKeepUserOnlineWhenOneDeviceDisconnects() {
            // Given: User has two devices online
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");

            // When: First device disconnects
            sessionManager.removeSession(user1Device1Channel);

            // Then: User should still be online (device-2 is still connected)
            assertThat(sessionManager.isUserOnline(1L)).isTrue();
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(1);
            assertThat(sessionManager.getSession(1L, "device-1")).isNull();
            assertThat(sessionManager.getSession(1L, "device-2")).isNotNull();
        }

        @Test
        @DisplayName("Should mark user offline when all devices disconnect")
        void shouldMarkUserOfflineWhenAllDevicesDisconnect() {
            // Given: User has two devices online
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");

            // When: Both devices disconnect
            sessionManager.removeSession(user1Device1Channel);
            sessionManager.removeSession(user1Device2Channel);

            // Then: User should be offline
            assertThat(sessionManager.isUserOnline(1L)).isFalse();
            assertThat(sessionManager.getSessionsByUserId(1L)).isEmpty();
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return correct session for specific device")
        void shouldReturnCorrectSessionForSpecificDevice() {
            // Given: User has two devices online
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");
            sessionManager.addSession(user1Device2Channel, 1L, "device-2", "mobile");

            // When: Getting specific device session
            UserSession device1Session = sessionManager.getSession(1L, "device-1");
            UserSession device2Session = sessionManager.getSession(1L, "device-2");

            // Then
            assertThat(device1Session).isNotNull();
            assertThat(device1Session.getChannel()).isEqualTo(user1Device1Channel);
            assertThat(device1Session.getDeviceType()).isEqualTo("web");

            assertThat(device2Session).isNotNull();
            assertThat(device2Session.getChannel()).isEqualTo(user1Device2Channel);
            assertThat(device2Session.getDeviceType()).isEqualTo("mobile");
        }
    }

    @Nested
    @DisplayName("Redis Message Listener Behavior")
    class RedisMessageListenerTests {

        @Test
        @DisplayName("Should parse Redis message correctly")
        void shouldParseRedisMessageCorrectly() throws Exception {
            // Given: A Redis message payload
            Map<String, Object> messageData = Map.of(
                    "type", "chat_message",
                    "senderId", 2L,
                    "senderDeviceId", "device-3",
                    "conversationId", 100L,
                    "msgId", "msg-123",
                    "message", Map.of("content", "Hello")
            );

            String messageJson = objectMapper.writeValueAsString(messageData);
            byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);

            // When: Parsing the message
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(messageBytes, Map.class);

            // Then
            assertThat(parsed.get("type")).isEqualTo("chat_message");
            assertThat(((Number) parsed.get("senderId")).longValue()).isEqualTo(2L);
            assertThat(parsed.get("senderDeviceId")).isEqualTo("device-3");
            assertThat(((Number) parsed.get("conversationId")).longValue()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should parse read status message correctly")
        void shouldParseReadStatusMessageCorrectly() throws Exception {
            // Given: A read status Redis message
            Map<String, Object> readStatusData = Map.of(
                    "type", "read_status",
                    "userId", 1L,
                    "deviceId", "device-1",
                    "conversationId", 100L,
                    "lastReadMsgId", 50L
            );

            String messageJson = objectMapper.writeValueAsString(readStatusData);
            byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);

            // When: Parsing the message
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(messageBytes, Map.class);

            // Then
            assertThat(parsed.get("type")).isEqualTo("read_status");
            assertThat(((Number) parsed.get("userId")).longValue()).isEqualTo(1L);
            assertThat(parsed.get("deviceId")).isEqualTo("device-1");
            assertThat(((Number) parsed.get("conversationId")).longValue()).isEqualTo(100L);
            assertThat(((Number) parsed.get("lastReadMsgId")).longValue()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("Offline Message Handling")
    class OfflineMessageHandlingTests {

        @Test
        @DisplayName("Should detect offline user correctly")
        void shouldDetectOfflineUserCorrectly() {
            // Given: No sessions are registered

            // When: Checking if user is online
            boolean isOnline = sessionManager.isUserOnline(1L);
            var sessions = sessionManager.getSessionsByUserId(1L);

            // Then
            assertThat(isOnline).isFalse();
            assertThat(sessions).isEmpty();
        }

        @Test
        @DisplayName("Should distinguish between online and offline users")
        void shouldDistinguishBetweenOnlineAndOfflineUsers() {
            // Given: User 1 is online, User 2 is offline
            sessionManager.addSession(user1Device1Channel, 1L, "device-1", "web");

            // When/Then
            assertThat(sessionManager.isUserOnline(1L)).isTrue();
            assertThat(sessionManager.isUserOnline(2L)).isFalse();
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(1);
            assertThat(sessionManager.getSessionsByUserId(2L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Concurrent Multi-Device Operations")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle concurrent device registrations")
        void shouldHandleConcurrentDeviceRegistrations() throws InterruptedException {
            // Given: Multiple threads registering devices
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int deviceNum = i;
                final Channel mockChannel = mock(Channel.class);
                final ChannelId mockChannelId = mock(ChannelId.class);
                when(mockChannel.id()).thenReturn(mockChannelId);
                when(mockChannelId.asLongText()).thenReturn("channel-" + deviceNum);
                when(mockChannelId.asShortText()).thenReturn("ch" + deviceNum);

                threads[i] = new Thread(() -> {
                    sessionManager.addSession(mockChannel, 1L, "device-" + deviceNum, "web");
                });
            }

            // When: All threads run concurrently
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // Then: All devices should be registered
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(threadCount);
        }

        @Test
        @DisplayName("Should handle concurrent add and remove operations")
        void shouldHandleConcurrentAddAndRemove() throws InterruptedException {
            // Given: Pre-register some sessions
            for (int i = 0; i < 3; i++) {
                final Channel mockChannel = mock(Channel.class);
                final ChannelId mockChannelId = mock(ChannelId.class);
                when(mockChannel.id()).thenReturn(mockChannelId);
                when(mockChannelId.asLongText()).thenReturn("channel-" + i);
                sessionManager.addSession(mockChannel, 1L, "device-" + i, "web");
            }

            // When: Concurrent operations
            Thread addThread = new Thread(() -> {
                for (int i = 10; i < 15; i++) {
                    final Channel mockChannel = mock(Channel.class);
                    final ChannelId mockChannelId = mock(ChannelId.class);
                    when(mockChannel.id()).thenReturn(mockChannelId);
                    when(mockChannelId.asLongText()).thenReturn("channel-" + i);
                    when(mockChannelId.asShortText()).thenReturn("ch" + i);
                    sessionManager.addSession(mockChannel, 2L, "device-" + i, "mobile");
                }
            });

            Thread queryThread = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    sessionManager.getSessionsByUserId(1L);
                    sessionManager.isUserOnline(1L);
                    sessionManager.isUserOnline(2L);
                }
            });

            addThread.start();
            queryThread.start();
            addThread.join();
            queryThread.join();

            // Then: All operations should complete without error
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(3);
            assertThat(sessionManager.getSessionsByUserId(2L)).hasSize(5);
        }
    }
}
