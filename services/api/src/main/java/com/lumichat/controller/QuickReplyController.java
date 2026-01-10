package com.lumichat.controller;

import com.lumichat.dto.request.QuickReplyRequest;
import com.lumichat.dto.request.ReorderQuickRepliesRequest;
import com.lumichat.dto.response.QuickReplyResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.QuickReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quick-replies")
@RequiredArgsConstructor
public class QuickReplyController {

    private final QuickReplyService quickReplyService;

    @GetMapping
    public ResponseEntity<List<QuickReplyResponse>> getQuickReplies(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<QuickReplyResponse> replies = quickReplyService.getQuickReplies(principal.getId());
        return ResponseEntity.ok(replies);
    }

    @PostMapping
    public ResponseEntity<QuickReplyResponse> createQuickReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuickReplyRequest request) {
        QuickReplyResponse reply = quickReplyService.createQuickReply(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuickReplyResponse> updateQuickReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuickReplyRequest request) {
        QuickReplyResponse reply = quickReplyService.updateQuickReply(id, principal.getId(), request);
        return ResponseEntity.ok(reply);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuickReply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        quickReplyService.deleteQuickReply(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<QuickReplyResponse>> reorderQuickReplies(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderQuickRepliesRequest request) {
        List<QuickReplyResponse> replies = quickReplyService.reorderQuickReplies(principal.getId(), request.getIds());
        return ResponseEntity.ok(replies);
    }
}
