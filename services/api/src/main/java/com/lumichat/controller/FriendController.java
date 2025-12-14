package com.lumichat.controller;

import com.lumichat.dto.request.SendFriendRequestRequest;
import com.lumichat.dto.request.UpdateMemoRequest;
import com.lumichat.dto.request.UpdateRemarkRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.FriendRequestResponse;
import com.lumichat.dto.response.FriendResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@Slf4j
public class FriendController {

    private final FriendService friendService;

    /**
     * Get all friends
     * GET /friends
     */
    @GetMapping
    public ApiResponse<List<FriendResponse>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean includeBlocked) {
        List<FriendResponse> friends = includeBlocked
                ? friendService.getAllFriends(principal.getId())
                : friendService.getFriends(principal.getId());
        return ApiResponse.success(friends);
    }

    /**
     * Send a friend request
     * POST /friends/request
     */
    @PostMapping("/request")
    public ApiResponse<FriendRequestResponse> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendFriendRequestRequest request) {
        FriendRequestResponse response = friendService.sendFriendRequest(principal.getId(), request);
        return ApiResponse.success(response);
    }

    /**
     * Get friend requests
     * GET /friends/requests
     */
    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestResponse>> getFriendRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean pendingOnly) {
        List<FriendRequestResponse> requests = pendingOnly
                ? friendService.getPendingFriendRequests(principal.getId())
                : friendService.getFriendRequests(principal.getId());
        return ApiResponse.success(requests);
    }

    /**
     * Accept a friend request
     * POST /friends/requests/{id}/accept
     */
    @PostMapping("/requests/{id}/accept")
    public ApiResponse<FriendRequestResponse> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        FriendRequestResponse response = friendService.acceptFriendRequest(principal.getId(), id);
        return ApiResponse.success(response);
    }

    /**
     * Reject a friend request
     * POST /friends/requests/{id}/reject
     */
    @PostMapping("/requests/{id}/reject")
    public ApiResponse<FriendRequestResponse> rejectFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        FriendRequestResponse response = friendService.rejectFriendRequest(principal.getId(), id);
        return ApiResponse.success(response);
    }

    /**
     * Delete a friend (unfriend)
     * DELETE /friends/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        friendService.deleteFriend(principal.getId(), id);
        return ApiResponse.success();
    }

    /**
     * Update friend remark
     * PUT /friends/{id}/remark
     */
    @PutMapping("/{id}/remark")
    public ApiResponse<Void> updateRemark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRemarkRequest request) {
        friendService.updateRemark(principal.getId(), id, request.getRemark());
        return ApiResponse.success();
    }

    /**
     * Update friend memo
     * PUT /friends/{id}/memo
     */
    @PutMapping("/{id}/memo")
    public ApiResponse<Void> updateMemo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemoRequest request) {
        friendService.updateMemo(principal.getId(), id, request.getMemo());
        return ApiResponse.success();
    }

    /**
     * Block a friend
     * POST /friends/{id}/block
     */
    @PostMapping("/{id}/block")
    public ApiResponse<Void> blockFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        friendService.blockFriend(principal.getId(), id);
        return ApiResponse.success();
    }

    /**
     * Unblock a friend
     * POST /friends/{id}/unblock
     */
    @PostMapping("/{id}/unblock")
    public ApiResponse<Void> unblockFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        friendService.unblockFriend(principal.getId(), id);
        return ApiResponse.success();
    }
}
