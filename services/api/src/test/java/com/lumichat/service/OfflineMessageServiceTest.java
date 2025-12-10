package com.lumichat.service;

import com.lumichat.dto.response.MessageResponse;
import com.lumichat.entity.*;
import com.lumichat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfflineMessageService Tests")
class OfflineMessageServiceTest {

    @Mock
    private OfflineMessageRepository offlineMessageRepository;

    @Mock
    private DeviceSyncStatusRepository deviceSyncStatusRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private OfflineMessageService offlineMessageService;

    private User testUser;
    private User otherUser;
    private Conversation conversation;
    private Message testMessage;
    private OfflineMessage offlineMessage;
    private DeviceSyncStatus syncStatus;

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

        testMessage = Message.builder()
                .id(500L)
                .msgId("msg-123456")
                .conversation(conversation)
                .sender(otherUser)
                .senderDeviceId("device-456")
                .msgType(Message.MessageType.text)
                .content("Hello World")
                .serverCreatedAt(LocalDateTime.now())
                .build();

        offlineMessage = OfflineMessage.builder()
                .id(1L)
                .targetUser(testUser)
                .targetDeviceId("device-123")
                .message(testMessage)
                .conversation(conversation)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(7))
                .retryCount(0)
                .build();

        syncStatus = DeviceSyncStatus.builder()
                .id(1L)
                .user(testUser)
                .deviceId("device-123")
                .lastSyncedMsgId(400L)
                .lastSyncedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Nested
    @DisplayName("QueueMessage Tests")
    class QueueMessageTests {

        @Test
        @DisplayName("Should queue message for offline user successfully")
        void shouldQueueMessageSuccessfully() {
            // Given
            when(offlineMessageRepository.existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(1L, 500L))
                    .thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.findById(500L)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(offlineMessageRepository.save(any(OfflineMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            offlineMessageService.queueMessage(1L, "device-123", 500L, 100L);

            // Then
            ArgumentCaptor<OfflineMessage> captor = ArgumentCaptor.forClass(OfflineMessage.class);
            verify(offlineMessageRepository).save(captor.capture());

            OfflineMessage saved = captor.getValue();
            assertThat(saved.getTargetUser().getId()).isEqualTo(1L);
            assertThat(saved.getTargetDeviceId()).isEqualTo("device-123");
            assertThat(saved.getMessage().getId()).isEqualTo(500L);
            assertThat(saved.getConversation().getId()).isEqualTo(100L);
            assertThat(saved.getExpiredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should skip queueing if message already queued")
        void shouldSkipIfAlreadyQueued() {
            // Given
            when(offlineMessageRepository.existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(1L, 500L))
                    .thenReturn(true);

            // When
            offlineMessageService.queueMessage(1L, "device-123", 500L, 100L);

            // Then
            verify(offlineMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when target user not found")
        void shouldThrowExceptionWhenTargetUserNotFound() {
            // Given
            when(offlineMessageRepository.existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(1L, 500L))
                    .thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> offlineMessageService.queueMessage(1L, "device-123", 500L, 100L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Target user not found");
        }

        @Test
        @DisplayName("Should throw exception when message not found")
        void shouldThrowExceptionWhenMessageNotFound() {
            // Given
            when(offlineMessageRepository.existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(1L, 500L))
                    .thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.findById(500L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> offlineMessageService.queueMessage(1L, "device-123", 500L, 100L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Message not found");
        }

        @Test
        @DisplayName("Should throw exception when conversation not found")
        void shouldThrowExceptionWhenConversationNotFound() {
            // Given
            when(offlineMessageRepository.existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(1L, 500L))
                    .thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.findById(500L)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findById(100L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> offlineMessageService.queueMessage(1L, "device-123", 500L, 100L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Conversation not found");
        }
    }

    @Nested
    @DisplayName("GetPendingMessages Tests")
    class GetPendingMessagesTests {

        @Test
        @DisplayName("Should get pending messages successfully")
        void shouldGetPendingMessagesSuccessfully() {
            // Given
            when(offlineMessageRepository.findPendingByUserIdAndDeviceId(
                    eq(1L), eq("device-123"), any(Pageable.class)))
                    .thenReturn(Arrays.asList(offlineMessage));

            // When
            List<MessageResponse> results = offlineMessageService.getPendingMessages(1L, "device-123", 100);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMsgId()).isEqualTo("msg-123456");
        }

        @Test
        @DisplayName("Should return empty list when no pending messages")
        void shouldReturnEmptyListWhenNoPendingMessages() {
            // Given
            when(offlineMessageRepository.findPendingByUserIdAndDeviceId(
                    eq(1L), eq("device-123"), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<MessageResponse> results = offlineMessageService.getPendingMessages(1L, "device-123", 100);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should use default limit when limit is zero or negative")
        void shouldUseDefaultLimitWhenInvalid() {
            // Given
            when(offlineMessageRepository.findPendingByUserIdAndDeviceId(
                    eq(1L), eq("device-123"), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            // When
            offlineMessageService.getPendingMessages(1L, "device-123", 0);

            // Then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(offlineMessageRepository).findPendingByUserIdAndDeviceId(
                    eq(1L), eq("device-123"), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100); // DEFAULT_BATCH_SIZE
        }
    }

    @Nested
    @DisplayName("GetPendingCount Tests")
    class GetPendingCountTests {

        @Test
        @DisplayName("Should return correct pending count")
        void shouldReturnCorrectPendingCount() {
            // Given
            when(offlineMessageRepository.countPendingByUserId(1L)).thenReturn(5L);

            // When
            long count = offlineMessageService.getPendingCount(1L);

            // Then
            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should return zero when no pending messages")
        void shouldReturnZeroWhenNoPending() {
            // Given
            when(offlineMessageRepository.countPendingByUserId(1L)).thenReturn(0L);

            // When
            long count = offlineMessageService.getPendingCount(1L);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("MarkAsDelivered Tests")
    class MarkAsDeliveredTests {

        @Test
        @DisplayName("Should mark messages as delivered")
        void shouldMarkMessagesAsDelivered() {
            // Given
            List<Long> ids = Arrays.asList(1L, 2L, 3L);

            // When
            offlineMessageService.markAsDelivered(ids);

            // Then
            verify(offlineMessageRepository).markAsDelivered(ids);
        }

        @Test
        @DisplayName("Should skip when ids list is empty")
        void shouldSkipWhenIdsEmpty() {
            // Given
            List<Long> ids = Collections.emptyList();

            // When
            offlineMessageService.markAsDelivered(ids);

            // Then
            verify(offlineMessageRepository, never()).markAsDelivered(any());
        }
    }

    @Nested
    @DisplayName("MarkAllDeliveredForDevice Tests")
    class MarkAllDeliveredForDeviceTests {

        @Test
        @DisplayName("Should mark all pending messages as delivered for device")
        void shouldMarkAllDeliveredForDevice() {
            // When
            offlineMessageService.markAllDeliveredForDevice(1L, "device-123");

            // Then
            verify(offlineMessageRepository).markAllDeliveredForDevice(1L, "device-123");
        }
    }

    @Nested
    @DisplayName("UpdateSyncStatus Tests")
    class UpdateSyncStatusTests {

        @Test
        @DisplayName("Should update existing sync status")
        void shouldUpdateExistingSyncStatus() {
            // Given
            when(deviceSyncStatusRepository.findByUserIdAndDeviceId(1L, "device-123"))
                    .thenReturn(Optional.of(syncStatus));
            when(deviceSyncStatusRepository.save(any(DeviceSyncStatus.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            offlineMessageService.updateSyncStatus(1L, "device-123", 600L);

            // Then
            ArgumentCaptor<DeviceSyncStatus> captor = ArgumentCaptor.forClass(DeviceSyncStatus.class);
            verify(deviceSyncStatusRepository).save(captor.capture());

            DeviceSyncStatus saved = captor.getValue();
            assertThat(saved.getLastSyncedMsgId()).isEqualTo(600L);
            assertThat(saved.getLastSyncedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create new sync status if not exists")
        void shouldCreateNewSyncStatusIfNotExists() {
            // Given
            when(deviceSyncStatusRepository.findByUserIdAndDeviceId(1L, "device-new"))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(deviceSyncStatusRepository.save(any(DeviceSyncStatus.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            offlineMessageService.updateSyncStatus(1L, "device-new", 600L);

            // Then
            ArgumentCaptor<DeviceSyncStatus> captor = ArgumentCaptor.forClass(DeviceSyncStatus.class);
            verify(deviceSyncStatusRepository).save(captor.capture());

            DeviceSyncStatus saved = captor.getValue();
            assertThat(saved.getDeviceId()).isEqualTo("device-new");
            assertThat(saved.getLastSyncedMsgId()).isEqualTo(600L);
        }

        @Test
        @DisplayName("Should throw exception when user not found for new sync status")
        void shouldThrowExceptionWhenUserNotFoundForNewSyncStatus() {
            // Given
            when(deviceSyncStatusRepository.findByUserIdAndDeviceId(1L, "device-new"))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> offlineMessageService.updateSyncStatus(1L, "device-new", 600L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("GetLastSyncedMessageId Tests")
    class GetLastSyncedMessageIdTests {

        @Test
        @DisplayName("Should return last synced message ID")
        void shouldReturnLastSyncedMessageId() {
            // Given
            when(deviceSyncStatusRepository.findByUserIdAndDeviceId(1L, "device-123"))
                    .thenReturn(Optional.of(syncStatus));

            // When
            Long result = offlineMessageService.getLastSyncedMessageId(1L, "device-123");

            // Then
            assertThat(result).isEqualTo(400L);
        }

        @Test
        @DisplayName("Should return null when no sync status exists")
        void shouldReturnNullWhenNoSyncStatus() {
            // Given
            when(deviceSyncStatusRepository.findByUserIdAndDeviceId(1L, "device-new"))
                    .thenReturn(Optional.empty());

            // When
            Long result = offlineMessageService.getLastSyncedMessageId(1L, "device-new");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("CleanupMessages Tests")
    class CleanupMessagesTests {

        @Test
        @DisplayName("Should cleanup expired and old delivered messages")
        void shouldCleanupMessages() {
            // Given
            when(offlineMessageRepository.deleteExpiredMessages(any(LocalDateTime.class))).thenReturn(5);
            when(offlineMessageRepository.deleteDeliveredBefore(any(LocalDateTime.class))).thenReturn(10);

            // When
            offlineMessageService.cleanupMessages();

            // Then
            verify(offlineMessageRepository).deleteExpiredMessages(any(LocalDateTime.class));
            verify(offlineMessageRepository).deleteDeliveredBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should handle zero deleted messages gracefully")
        void shouldHandleZeroDeletedMessages() {
            // Given
            when(offlineMessageRepository.deleteExpiredMessages(any(LocalDateTime.class))).thenReturn(0);
            when(offlineMessageRepository.deleteDeliveredBefore(any(LocalDateTime.class))).thenReturn(0);

            // When
            offlineMessageService.cleanupMessages();

            // Then
            verify(offlineMessageRepository).deleteExpiredMessages(any(LocalDateTime.class));
            verify(offlineMessageRepository).deleteDeliveredBefore(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("OfflineMessage Entity Tests")
    class OfflineMessageEntityTests {

        @Test
        @DisplayName("Should check if message is delivered")
        void shouldCheckIfDelivered() {
            // Given
            OfflineMessage undelivered = OfflineMessage.builder().build();
            OfflineMessage delivered = OfflineMessage.builder()
                    .deliveredAt(LocalDateTime.now())
                    .build();

            // When/Then
            assertThat(undelivered.isDelivered()).isFalse();
            assertThat(delivered.isDelivered()).isTrue();
        }

        @Test
        @DisplayName("Should check if message is expired")
        void shouldCheckIfExpired() {
            // Given
            OfflineMessage notExpired = OfflineMessage.builder()
                    .expiredAt(LocalDateTime.now().plusDays(1))
                    .build();
            OfflineMessage expired = OfflineMessage.builder()
                    .expiredAt(LocalDateTime.now().minusDays(1))
                    .build();
            OfflineMessage noExpiry = OfflineMessage.builder().build();

            // When/Then
            assertThat(notExpired.isExpired()).isFalse();
            assertThat(expired.isExpired()).isTrue();
            assertThat(noExpiry.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should mark message as delivered")
        void shouldMarkAsDelivered() {
            // Given
            OfflineMessage msg = OfflineMessage.builder().build();
            assertThat(msg.getDeliveredAt()).isNull();

            // When
            msg.markDelivered();

            // Then
            assertThat(msg.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should increment retry count")
        void shouldIncrementRetryCount() {
            // Given
            OfflineMessage msg = OfflineMessage.builder().retryCount(0).build();

            // When
            msg.incrementRetryCount();
            msg.incrementRetryCount();

            // Then
            assertThat(msg.getRetryCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("DeviceSyncStatus Entity Tests")
    class DeviceSyncStatusEntityTests {

        @Test
        @DisplayName("Should update sync status")
        void shouldUpdateSyncStatus() {
            // Given
            DeviceSyncStatus status = DeviceSyncStatus.builder()
                    .lastSyncedMsgId(100L)
                    .build();

            // When
            status.updateSyncStatus(200L);

            // Then
            assertThat(status.getLastSyncedMsgId()).isEqualTo(200L);
            assertThat(status.getLastSyncedAt()).isNotNull();
        }
    }
}
