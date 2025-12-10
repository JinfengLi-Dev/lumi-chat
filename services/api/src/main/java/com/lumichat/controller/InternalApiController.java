package com.lumichat.controller;

import com.lumichat.dto.request.SendMessageRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.entity.Conversation;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.security.InternalServiceFilter.InternalServicePrincipal;
import com.lumichat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lumichat.entity.Conversation.ConversationType;
import com.lumichat.service.ConversationService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal API endpoints for service-to-service communication.
 * These endpoints are authenticated via the InternalServiceFilter.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalApiController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;

    /**
     * Persist a message from the IM server.
     * POST /internal/messages
     */
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> persistMessage(
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {

        if (principal == null || principal.userId() == null) {
            return ApiResponse.error(400, "User ID is required");
        }

        log.info("Internal message persist request from {} for user {}",
                principal.serviceName(), principal.userId());

        MessageResponse message = messageService.sendMessage(
                principal.userId(),
                principal.deviceId(),
                request
        );

        return ApiResponse.success(message);
    }

    /**
     * Recall a message from the IM server.
     * PUT /internal/messages/{msgId}/recall
     */
    @PutMapping("/messages/{msgId}/recall")
    public ApiResponse<Void> recallMessage(
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @PathVariable String msgId) {

        if (principal == null || principal.userId() == null) {
            return ApiResponse.error(400, "User ID is required");
        }

        log.info("Internal recall request from {} for user {}, msgId={}",
                principal.serviceName(), principal.userId(), msgId);

        messageService.recallMessage(principal.userId(), msgId);
        return ApiResponse.success();
    }

    /**
     * Get conversation participants.
     * GET /internal/conversations/{conversationId}/participants
     */
    @GetMapping("/conversations/{conversationId}/participants")
    public ApiResponse<List<Long>> getConversationParticipants(
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @PathVariable Long conversationId) {

        log.debug("Internal participants request from {} for conversation {}",
                principal != null ? principal.serviceName() : "unknown", conversationId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null) {
            return ApiResponse.error(404, "Conversation not found");
        }

        List<Long> participants = conversation.getParticipantIds() != null
                ? Arrays.asList(conversation.getParticipantIds())
                : List.of();

        return ApiResponse.success(participants);
    }

    /**
     * Get messages for sync.
     * GET /internal/conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> getMessagesForSync(
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "50") int limit) {

        if (principal == null || principal.userId() == null) {
            return ApiResponse.error(400, "User ID is required");
        }

        log.debug("Internal messages sync request from {} for user {}, conversation={}",
                principal.serviceName(), principal.userId(), conversationId);

        List<MessageResponse> messages = messageService.getMessages(
                principal.userId(),
                conversationId,
                afterId > 0 ? afterId : null,
                Math.min(limit, 100)
        );

        return ApiResponse.success(messages);
    }

    /**
     * Update read status for a conversation from the IM server.
     * Returns info needed to notify message senders (for private chats).
     * POST /internal/conversations/{conversationId}/read
     */
    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<Map<String, Object>> updateReadStatus(
            @AuthenticationPrincipal InternalServicePrincipal principal,
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> request) {

        if (principal == null || principal.userId() == null) {
            return ApiResponse.error(400, "User ID is required");
        }

        Long lastReadMsgId = request.get("lastReadMsgId") != null
                ? ((Number) request.get("lastReadMsgId")).longValue()
                : null;

        if (lastReadMsgId == null) {
            return ApiResponse.error(400, "lastReadMsgId is required");
        }

        log.info("Internal read status update from {} for user {}, conversation={}, lastReadMsgId={}",
                principal.serviceName(), principal.userId(), conversationId, lastReadMsgId);

        // Mark conversation as read in the user_conversations table
        conversationService.markAsRead(principal.userId(), conversationId);

        // Get conversation info to determine if we should broadcast
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null) {
            return ApiResponse.error(404, "Conversation not found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("conversationId", conversationId);
        result.put("lastReadMsgId", lastReadMsgId);
        result.put("readerId", principal.userId());

        // For private chats, include the other participant to notify
        if (conversation.getType() == ConversationType.private_chat) {
            List<Long> participants = conversation.getParticipantIds() != null
                    ? Arrays.asList(conversation.getParticipantIds())
                    : List.of();

            // Find the other participant (message sender) to notify
            Long otherUserId = participants.stream()
                    .filter(id -> !id.equals(principal.userId()))
                    .findFirst()
                    .orElse(null);

            if (otherUserId != null) {
                result.put("notifyUserId", otherUserId);
            }
        }

        return ApiResponse.success(result);
    }
}
