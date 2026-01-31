package com.lumichat.controller;

import com.lumichat.dto.request.AddReactionRequest;
import com.lumichat.dto.request.SendMessageRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.dto.response.ReactionResponse;
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
     * Search messages in a conversation
     * GET /conversations/{conversationId}/messages/search?q=xxx
     */
    @GetMapping("/conversations/{conversationId}/messages/search")
    public ApiResponse<List<MessageResponse>> searchMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long conversationId,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<MessageResponse> messages = messageService.searchMessages(
                principal.getId(), conversationId, query, page, Math.min(limit, 100));
        return ApiResponse.success(messages);
    }

    /**
     * Get media messages (images, videos, files) for a conversation
     * GET /conversations/{conversationId}/media?type=image
     */
    @GetMapping("/conversations/{conversationId}/media")
    public ApiResponse<List<MessageResponse>> getMediaMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "image") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<MessageResponse> messages = messageService.getMediaMessages(
                principal.getId(), conversationId, type, page, Math.min(limit, 50));
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

    /**
     * Add a reaction to a message
     * POST /messages/{messageId}/reactions
     */
    @PostMapping("/messages/{messageId}/reactions")
    public ApiResponse<Void> addReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId,
            @Valid @RequestBody AddReactionRequest request) {
        messageService.addReaction(principal.getId(), messageId, request.getEmoji());
        return ApiResponse.success();
    }

    /**
     * Remove a reaction from a message
     * DELETE /messages/{messageId}/reactions/{emoji}
     */
    @DeleteMapping("/messages/{messageId}/reactions/{emoji}")
    public ApiResponse<Void> removeReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId,
            @PathVariable String emoji) {
        messageService.removeReaction(principal.getId(), messageId, emoji);
        return ApiResponse.success();
    }

    /**
     * Get all reactions for a message
     * GET /messages/{messageId}/reactions
     */
    @GetMapping("/messages/{messageId}/reactions")
    public ApiResponse<List<ReactionResponse>> getReactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId) {
        List<ReactionResponse> reactions = messageService.getReactions(principal.getId(), messageId);
        return ApiResponse.success(reactions);
    }

    // Inner class for forward request
    public record ForwardRequest(Long targetConversationId) {}
}
