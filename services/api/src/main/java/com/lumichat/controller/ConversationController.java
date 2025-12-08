package com.lumichat.controller;

import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.ConversationResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Get all conversations for current user
     * GET /conversations
     */
    @GetMapping
    public ApiResponse<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ConversationResponse> conversations = conversationService.getUserConversations(principal.getId());
        return ApiResponse.success(conversations);
    }

    /**
     * Get a single conversation
     * GET /conversations/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        ConversationResponse conversation = conversationService.getConversation(principal.getId(), id);
        return ApiResponse.success(conversation);
    }

    /**
     * Create or get private conversation with another user
     * POST /conversations/private
     */
    @PostMapping("/private")
    public ApiResponse<ConversationResponse> createPrivateConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreatePrivateConversationRequest request) {
        ConversationResponse conversation = conversationService.getOrCreatePrivateConversation(
                principal.getId(), request.targetUserId());
        return ApiResponse.success(conversation);
    }

    /**
     * Delete a conversation
     * DELETE /conversations/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        conversationService.deleteConversation(principal.getId(), id);
        return ApiResponse.success();
    }

    /**
     * Mark conversation as read
     * POST /conversations/{id}/read
     */
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        conversationService.markAsRead(principal.getId(), id);
        return ApiResponse.success();
    }

    /**
     * Toggle mute status
     * PUT /conversations/{id}/mute
     */
    @PutMapping("/{id}/mute")
    public ApiResponse<Void> toggleMute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody MuteRequest request) {
        conversationService.toggleMute(principal.getId(), id, request.muted());
        return ApiResponse.success();
    }

    /**
     * Toggle pin status
     * PUT /conversations/{id}/pin
     */
    @PutMapping("/{id}/pin")
    public ApiResponse<Void> togglePin(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody PinRequest request) {
        conversationService.togglePin(principal.getId(), id, request.pinned());
        return ApiResponse.success();
    }

    /**
     * Save draft
     * PUT /conversations/{id}/draft
     */
    @PutMapping("/{id}/draft")
    public ApiResponse<Void> saveDraft(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody DraftRequest request) {
        conversationService.saveDraft(principal.getId(), id, request.draft());
        return ApiResponse.success();
    }

    /**
     * Clear messages in a conversation
     * DELETE /conversations/{id}/messages
     */
    @DeleteMapping("/{id}/messages")
    public ApiResponse<Void> clearMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        conversationService.clearMessages(principal.getId(), id);
        return ApiResponse.success();
    }

    // Inner classes for simple request bodies
    public record CreatePrivateConversationRequest(Long targetUserId) {}
    public record MuteRequest(boolean muted) {}
    public record PinRequest(boolean pinned) {}
    public record DraftRequest(String draft) {}
}
