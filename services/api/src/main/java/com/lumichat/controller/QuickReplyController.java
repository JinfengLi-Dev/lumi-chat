package com.lumichat.controller;

import com.lumichat.dto.request.QuickReplyRequest;
import com.lumichat.dto.request.ReorderQuickRepliesRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.QuickReplyResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.QuickReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quick-replies")
@RequiredArgsConstructor
public class QuickReplyController {

    private final QuickReplyService quickReplyService;

    @GetMapping
    public ApiResponse<List<QuickReplyResponse>> getQuickReplies(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<QuickReplyResponse> replies = quickReplyService.getQuickReplies(principal.getId());
        return ApiResponse.success(replies);
    }

    @PostMapping
    public ApiResponse<QuickReplyResponse> createQuickReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuickReplyRequest request) {
        QuickReplyResponse reply = quickReplyService.createQuickReply(principal.getId(), request);
        return ApiResponse.success(reply);
    }

    @PutMapping("/{id}")
    public ApiResponse<QuickReplyResponse> updateQuickReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuickReplyRequest request) {
        QuickReplyResponse reply = quickReplyService.updateQuickReply(id, principal.getId(), request);
        return ApiResponse.success(reply);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteQuickReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        quickReplyService.deleteQuickReply(id, principal.getId());
        return ApiResponse.success();
    }

    @PutMapping("/reorder")
    public ApiResponse<List<QuickReplyResponse>> reorderQuickReplies(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderQuickRepliesRequest request) {
        List<QuickReplyResponse> replies = quickReplyService.reorderQuickReplies(principal.getId(), request.getIds());
        return ApiResponse.success(replies);
    }
}
