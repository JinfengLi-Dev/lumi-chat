package com.lumichat.im.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionManager Tests")
class SessionManagerTest {

    private SessionManager sessionManager;

    @Mock
    private Channel channel1;

    @Mock
    private Channel channel2;

    @Mock
    private Channel channel3;

    @Mock
    private ChannelId channelId1;

    @Mock
    private ChannelId channelId2;

    @Mock
    private ChannelId channelId3;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();

        // Setup channel mocks
        lenient().when(channel1.id()).thenReturn(channelId1);
        lenient().when(channelId1.asLongText()).thenReturn("channel-1-long");
        lenient().when(channelId1.asShortText()).thenReturn("ch1");

        lenient().when(channel2.id()).thenReturn(channelId2);
        lenient().when(channelId2.asLongText()).thenReturn("channel-2-long");
        lenient().when(channelId2.asShortText()).thenReturn("ch2");

        lenient().when(channel3.id()).thenReturn(channelId3);
        lenient().when(channelId3.asLongText()).thenReturn("channel-3-long");
        lenient().when(channelId3.asShortText()).thenReturn("ch3");
    }

    @Nested
    @DisplayName("Add Session Tests")
    class AddSessionTests {

        @Test
        @DisplayName("Should add session and retrieve by channel")
        void shouldAddSessionAndRetrieveByChannel() {
            // When
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // Then
            UserSession session = sessionManager.getSessionByChannel(channel1);
            assertThat(session).isNotNull();
            assertThat(session.getUserId()).isEqualTo(1L);
            assertThat(session.getDeviceId()).isEqualTo("device-1");
            assertThat(session.getDeviceType()).isEqualTo("web");
            assertThat(session.getChannel()).isEqualTo(channel1);
            assertThat(session.getConnectedAt()).isNotNull();
            assertThat(session.getLastActiveAt()).isNotNull();
        }

        @Test
        @DisplayName("Should add session and retrieve by userId")
        void shouldAddSessionAndRetrieveByUserId() {
            // When
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // Then
            Collection<UserSession> sessions = sessionManager.getSessionsByUserId(1L);
            assertThat(sessions).hasSize(1);
            assertThat(sessions.iterator().next().getDeviceId()).isEqualTo("device-1");
        }

        @Test
        @DisplayName("Should add session and retrieve by userId and deviceId")
        void shouldAddSessionAndRetrieveByUserIdAndDeviceId() {
            // When
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // Then
            UserSession session = sessionManager.getSession(1L, "device-1");
            assertThat(session).isNotNull();
            assertThat(session.getChannel()).isEqualTo(channel1);
        }
    }

    @Nested
    @DisplayName("Multi-Device Session Tests")
    class MultiDeviceSessionTests {

        @Test
        @DisplayName("Should track multiple devices for same user")
        void shouldTrackMultipleDevicesForSameUser() {
            // When
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            sessionManager.addSession(channel2, 1L, "device-2", "mobile");

            // Then
            Collection<UserSession> sessions = sessionManager.getSessionsByUserId(1L);
            assertThat(sessions).hasSize(2);
        }

        @Test
        @DisplayName("Should retrieve correct session for each device")
        void shouldRetrieveCorrectSessionForEachDevice() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            sessionManager.addSession(channel2, 1L, "device-2", "mobile");

            // When
            UserSession session1 = sessionManager.getSession(1L, "device-1");
            UserSession session2 = sessionManager.getSession(1L, "device-2");

            // Then
            assertThat(session1.getChannel()).isEqualTo(channel1);
            assertThat(session1.getDeviceType()).isEqualTo("web");
            assertThat(session2.getChannel()).isEqualTo(channel2);
            assertThat(session2.getDeviceType()).isEqualTo("mobile");
        }

        @Test
        @DisplayName("Should track sessions for different users")
        void shouldTrackSessionsForDifferentUsers() {
            // When
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            sessionManager.addSession(channel2, 2L, "device-2", "mobile");

            // Then
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(1);
            assertThat(sessionManager.getSessionsByUserId(2L)).hasSize(1);
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Remove Session Tests")
    class RemoveSessionTests {

        @Test
        @DisplayName("Should remove session by channel")
        void shouldRemoveSessionByChannel() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // When
            sessionManager.removeSession(channel1);

            // Then
            assertThat(sessionManager.getSessionByChannel(channel1)).isNull();
            assertThat(sessionManager.getSessionsByUserId(1L)).isEmpty();
            assertThat(sessionManager.getSession(1L, "device-1")).isNull();
        }

        @Test
        @DisplayName("Should remove only specific device session")
        void shouldRemoveOnlySpecificDeviceSession() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            sessionManager.addSession(channel2, 1L, "device-2", "mobile");

            // When
            sessionManager.removeSession(channel1);

            // Then
            assertThat(sessionManager.getSessionByChannel(channel1)).isNull();
            assertThat(sessionManager.getSession(1L, "device-1")).isNull();
            assertThat(sessionManager.getSession(1L, "device-2")).isNotNull();
            assertThat(sessionManager.getSessionsByUserId(1L)).hasSize(1);
        }

        @Test
        @DisplayName("Should remove user from map when last device disconnects")
        void shouldRemoveUserFromMapWhenLastDeviceDisconnects() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // When
            sessionManager.removeSession(channel1);

            // Then
            assertThat(sessionManager.isUserOnline(1L)).isFalse();
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle removing non-existent session gracefully")
        void shouldHandleRemovingNonExistentSessionGracefully() {
            // When - Should not throw
            sessionManager.removeSession(channel1);

            // Then - Nothing to assert, just verifying no exception
        }
    }

    @Nested
    @DisplayName("Online Status Tests")
    class OnlineStatusTests {

        @Test
        @DisplayName("Should return true for online user")
        void shouldReturnTrueForOnlineUser() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // Then
            assertThat(sessionManager.isUserOnline(1L)).isTrue();
        }

        @Test
        @DisplayName("Should return false for offline user")
        void shouldReturnFalseForOfflineUser() {
            // Then
            assertThat(sessionManager.isUserOnline(1L)).isFalse();
        }

        @Test
        @DisplayName("Should return false after user logs out")
        void shouldReturnFalseAfterUserLogsOut() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // When
            sessionManager.removeSession(channel1);

            // Then
            assertThat(sessionManager.isUserOnline(1L)).isFalse();
        }

        @Test
        @DisplayName("Should count online users correctly")
        void shouldCountOnlineUsersCorrectly() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            sessionManager.addSession(channel2, 2L, "device-2", "mobile");

            // Then
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(2);

            // When
            sessionManager.removeSession(channel1);

            // Then
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get All Sessions Tests")
    class GetAllSessionsTests {

        @Test
        @DisplayName("Should return all sessions")
        void shouldReturnAllSessions() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            sessionManager.addSession(channel2, 1L, "device-2", "mobile");
            sessionManager.addSession(channel3, 2L, "device-3", "desktop");

            // When
            Collection<UserSession> allSessions = sessionManager.getAllSessions();

            // Then
            assertThat(allSessions).hasSize(3);
        }

        @Test
        @DisplayName("Should return empty collection when no sessions")
        void shouldReturnEmptyCollectionWhenNoSessions() {
            // When
            Collection<UserSession> allSessions = sessionManager.getAllSessions();

            // Then
            assertThat(allSessions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Last Active Tests")
    class UpdateLastActiveTests {

        @Test
        @DisplayName("Should update last active timestamp")
        void shouldUpdateLastActiveTimestamp() throws InterruptedException {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");
            UserSession session = sessionManager.getSessionByChannel(channel1);
            var originalLastActive = session.getLastActiveAt();

            // Wait a bit to ensure different timestamp
            Thread.sleep(10);

            // When
            sessionManager.updateLastActive(channel1);

            // Then
            assertThat(session.getLastActiveAt()).isAfter(originalLastActive);
        }

        @Test
        @DisplayName("Should handle updating non-existent channel gracefully")
        void shouldHandleUpdatingNonExistentChannelGracefully() {
            // When - Should not throw
            sessionManager.updateLastActive(channel1);

            // Then - Nothing to assert, just verifying no exception
        }
    }

    @Nested
    @DisplayName("Query Edge Cases Tests")
    class QueryEdgeCasesTests {

        @Test
        @DisplayName("Should return null for unknown channel")
        void shouldReturnNullForUnknownChannel() {
            // Then
            assertThat(sessionManager.getSessionByChannel(channel1)).isNull();
        }

        @Test
        @DisplayName("Should return empty collection for unknown userId")
        void shouldReturnEmptyCollectionForUnknownUserId() {
            // Then
            assertThat(sessionManager.getSessionsByUserId(999L)).isEmpty();
        }

        @Test
        @DisplayName("Should return null for unknown userId and deviceId")
        void shouldReturnNullForUnknownUserIdAndDeviceId() {
            // Then
            assertThat(sessionManager.getSession(999L, "unknown-device")).isNull();
        }

        @Test
        @DisplayName("Should return null for known userId but unknown deviceId")
        void shouldReturnNullForKnownUserIdButUnknownDeviceId() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // Then
            assertThat(sessionManager.getSession(1L, "unknown-device")).isNull();
        }
    }

    @Nested
    @DisplayName("Clean Inactive Sessions Tests")
    class CleanInactiveSessionsTests {

        @Test
        @DisplayName("Should close inactive sessions")
        void shouldCloseInactiveSessions() throws InterruptedException {
            // Given
            ChannelFuture channelFuture = mock(ChannelFuture.class);
            when(channel1.close()).thenReturn(channelFuture);

            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // Make session inactive by waiting and not updating lastActive
            Thread.sleep(50);

            // When - Clean sessions inactive for more than 20ms
            sessionManager.cleanInactiveSessions(20);

            // Then
            verify(channel1).close();
        }

        @Test
        @DisplayName("Should not close active sessions")
        void shouldNotCloseActiveSessions() {
            // Given
            sessionManager.addSession(channel1, 1L, "device-1", "web");

            // When - Clean sessions inactive for more than 1 hour (session is recent)
            sessionManager.cleanInactiveSessions(3600000);

            // Then
            verify(channel1, never()).close();
        }

        @Test
        @DisplayName("Should only close inactive sessions")
        void shouldOnlyCloseInactiveSessions() throws InterruptedException {
            // Given
            ChannelFuture channelFuture = mock(ChannelFuture.class);
            when(channel1.close()).thenReturn(channelFuture);

            sessionManager.addSession(channel1, 1L, "device-1", "web");
            Thread.sleep(50);
            sessionManager.addSession(channel2, 2L, "device-2", "mobile"); // Added later

            // When - Clean sessions inactive for more than 30ms
            sessionManager.cleanInactiveSessions(30);

            // Then - Only channel1 should be closed
            verify(channel1).close();
            verify(channel2, never()).close();
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent session additions")
        void shouldHandleConcurrentSessionAdditions() throws InterruptedException {
            // Given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int userId = i;
                final Channel mockChannel = mock(Channel.class);
                final ChannelId mockChannelId = mock(ChannelId.class);
                when(mockChannel.id()).thenReturn(mockChannelId);
                when(mockChannelId.asLongText()).thenReturn("channel-" + userId);
                when(mockChannelId.asShortText()).thenReturn("ch" + userId);

                threads[i] = new Thread(() -> {
                    sessionManager.addSession(mockChannel, (long) userId, "device-" + userId, "web");
                });
            }

            // When
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(threadCount);
        }
    }
}
