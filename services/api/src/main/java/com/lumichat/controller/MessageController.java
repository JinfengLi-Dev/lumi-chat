package com.lumichat.controller;

import com.lumichat.dto.request.SendMessageRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    /**
     * Get messages for a conversation
     * GET /conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "20") int limit) {
        List<MessageResponse> messages = messageService.getMessages(
                principal.getId(), conversationId, before, Math.min(limit, 100));
        return ApiResponse.success(messages);
    }

    /**
     * Send a message
     * POST /messages
     */
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse message = messageService.sendMessage(
                principal.getId(), principal.getDeviceId(), request);
        return ApiResponse.success(message);
    }

    /**
     * Recall a message
     * PUT /messages/{msgId}/recall
     */
    @PutMapping("/messages/{msgId}/recall")
    public ApiResponse<Void> recallMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String msgId) {
        messageService.recallMessage(principal.getId(), msgId);
        return ApiResponse.success();
    }

    /**
     * Forward a message
     * POST /messages/{msgId}/forward
     */
    @PostMapping("/messages/{msgId}/forward")
    public ApiResponse<MessageResponse> forwardMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String msgId,
            @RequestBody ForwardRequest request) {
        MessageResponse message = messageService.forwardMessage(
                principal.getId(), principal.getDeviceId(), msgId, request.targetConversationId());
        return ApiResponse.success(message);
    }

    /**
     * Delete a message
     * DELETE /messages/{msgId}
     */
    @DeleteMapping("/messages/{msgId}")
    public ApiResponse<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String msgId) {
        messageService.deleteMessage(principal.getId(), msgId);
        return ApiResponse.success();
    }

    // Inner class for forward request
    public record ForwardRequest(Long targetConversationId) {}
}
