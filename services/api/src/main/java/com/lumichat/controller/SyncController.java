package com.lumichat.controller;

import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.OfflineMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for device synchronization and offline message delivery
 */
@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final OfflineMessageService offlineMessageService;

    /**
     * Get pending offline messages for the current device
     * Called after login or reconnection
     * GET /sync/messages
     */
    @GetMapping("/messages")
    public ApiResponse<SyncMessagesResponse> getPendingMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "100") int limit) {

        List<MessageResponse> messages = offlineMessageService.getPendingMessages(
                principal.getId(),
                principal.getDeviceId(),
                Math.min(limit, 500));

        long totalPending = offlineMessageService.getPendingCount(principal.getId());

        return ApiResponse.success(new SyncMessagesResponse(
                messages,
                totalPending,
                messages.size() < totalPending));
    }

    /**
     * Acknowledge receipt of synced messages
     * Called after client has processed pending messages
     * POST /sync/ack
     */
    @PostMapping("/ack")
    public ApiResponse<Void> acknowledgeSyncedMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SyncAckRequest request) {

        if (request.markAllDelivered()) {
            offlineMessageService.markAllDeliveredForDevice(
                    principal.getId(),
                    principal.getDeviceId());
        } else if (request.offlineMessageIds() != null && !request.offlineMessageIds().isEmpty()) {
            offlineMessageService.markAsDelivered(request.offlineMessageIds());
        }

        // Update sync status if lastMessageId provided
        if (request.lastMessageId() != null) {
            offlineMessageService.updateSyncStatus(
                    principal.getId(),
                    principal.getDeviceId(),
                    request.lastMessageId());
        }

        return ApiResponse.success();
    }

    /**
     * Get sync status for current device
     * GET /sync/status
     */
    @GetMapping("/status")
    public ApiResponse<SyncStatusResponse> getSyncStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long lastSyncedMsgId = offlineMessageService.getLastSyncedMessageId(
                principal.getId(),
                principal.getDeviceId());

        long pendingCount = offlineMessageService.getPendingCount(principal.getId());

        return ApiResponse.success(new SyncStatusResponse(
                lastSyncedMsgId,
                pendingCount,
                pendingCount > 0));
    }

    /**
     * Queue a message for offline delivery (called by IM server)
     * POST /sync/queue
     * Note: This endpoint should be internal/protected
     */
    @PostMapping("/queue")
    public ApiResponse<Void> queueOfflineMessage(@RequestBody QueueMessageRequest request) {
        offlineMessageService.queueMessage(
                request.targetUserId(),
                request.targetDeviceId(),
                request.messageId(),
                request.conversationId());
        return ApiResponse.success();
    }

    // Response DTOs
    public record SyncMessagesResponse(
            List<MessageResponse> messages,
            long totalPending,
            boolean hasMore) {}

    public record SyncStatusResponse(
            Long lastSyncedMessageId,
            long pendingCount,
            boolean hasPendingMessages) {}

    // Request DTOs
    public record SyncAckRequest(
            List<Long> offlineMessageIds,
            Long lastMessageId,
            boolean markAllDelivered) {}

    public record QueueMessageRequest(
            Long targetUserId,
            String targetDeviceId,
            Long messageId,
            Long conversationId) {}
}
