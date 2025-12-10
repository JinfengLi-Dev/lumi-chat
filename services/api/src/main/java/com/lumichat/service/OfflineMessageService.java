package com.lumichat.service;

import com.lumichat.dto.response.MessageResponse;
import com.lumichat.entity.*;
import com.lumichat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineMessageService {

    private final OfflineMessageRepository offlineMessageRepository;
    private final DeviceSyncStatusRepository deviceSyncStatusRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MESSAGE_EXPIRY_DAYS = 7;

    /**
     * Queue a message for an offline user/device
     */
    @Transactional
    public void queueMessage(Long targetUserId, String targetDeviceId, Long messageId, Long conversationId) {
        // Check if already queued
        if (offlineMessageRepository.existsByTargetUserIdAndMessageIdAndDeliveredAtIsNull(
                targetUserId, messageId)) {
            log.debug("Message {} already queued for user {}", messageId, targetUserId);
            return;
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        OfflineMessage offlineMessage = OfflineMessage.builder()
                .targetUser(targetUser)
                .targetDeviceId(targetDeviceId)
                .message(message)
                .conversation(conversation)
                .expiredAt(LocalDateTime.now().plusDays(MESSAGE_EXPIRY_DAYS))
                .build();

        offlineMessageRepository.save(offlineMessage);
        log.info("Queued message {} for offline user {} device {}",
                messageId, targetUserId, targetDeviceId);
    }

    /**
     * Get pending messages for a user/device
     */
    public List<MessageResponse> getPendingMessages(Long userId, String deviceId, int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_BATCH_SIZE;

        List<OfflineMessage> pendingMessages = offlineMessageRepository
                .findPendingByUserIdAndDeviceId(userId, deviceId, PageRequest.of(0, effectiveLimit));

        return pendingMessages.stream()
                .map(om -> MessageResponse.fromWithSender(om.getMessage()))
                .toList();
    }

    /**
     * Get pending message count for a user
     */
    public long getPendingCount(Long userId) {
        return offlineMessageRepository.countPendingByUserId(userId);
    }

    /**
     * Mark specific offline messages as delivered
     */
    @Transactional
    public void markAsDelivered(List<Long> offlineMessageIds) {
        if (offlineMessageIds.isEmpty()) {
            return;
        }
        offlineMessageRepository.markAsDelivered(offlineMessageIds);
        log.info("Marked {} offline messages as delivered", offlineMessageIds.size());
    }

    /**
     * Mark all pending messages for a user/device as delivered
     */
    @Transactional
    public void markAllDeliveredForDevice(Long userId, String deviceId) {
        offlineMessageRepository.markAllDeliveredForDevice(userId, deviceId);
        log.info("Marked all pending messages as delivered for user {} device {}", userId, deviceId);
    }

    /**
     * Update device sync status after successful sync
     */
    @Transactional
    public void updateSyncStatus(Long userId, String deviceId, Long lastMessageId) {
        DeviceSyncStatus syncStatus = deviceSyncStatusRepository
                .findByUserIdAndDeviceId(userId, deviceId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return DeviceSyncStatus.builder()
                            .user(user)
                            .deviceId(deviceId)
                            .build();
                });

        syncStatus.updateSyncStatus(lastMessageId);
        deviceSyncStatusRepository.save(syncStatus);
        log.debug("Updated sync status for user {} device {} to message {}",
                userId, deviceId, lastMessageId);
    }

    /**
     * Get last synced message ID for a device
     */
    public Long getLastSyncedMessageId(Long userId, String deviceId) {
        return deviceSyncStatusRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(DeviceSyncStatus::getLastSyncedMsgId)
                .orElse(null);
    }

    /**
     * Cleanup expired and old delivered messages
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupMessages() {
        LocalDateTime now = LocalDateTime.now();

        // Delete expired messages
        int expiredCount = offlineMessageRepository.deleteExpiredMessages(now);
        if (expiredCount > 0) {
            log.info("Deleted {} expired offline messages", expiredCount);
        }

        // Delete delivered messages older than 24 hours
        LocalDateTime oneDayAgo = now.minusDays(1);
        int deliveredCount = offlineMessageRepository.deleteDeliveredBefore(oneDayAgo);
        if (deliveredCount > 0) {
            log.info("Deleted {} old delivered offline messages", deliveredCount);
        }
    }
}
