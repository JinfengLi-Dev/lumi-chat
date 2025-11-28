package com.lumichat.controller;

import com.lumichat.dto.request.AddGroupMembersRequest;
import com.lumichat.dto.request.CreateGroupRequest;
import com.lumichat.dto.request.TransferOwnershipRequest;
import com.lumichat.dto.request.UpdateGroupRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.GroupDetailResponse;
import com.lumichat.dto.response.GroupMemberResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;

    /**
     * Get all groups for current user
     * GET /groups
     */
    @GetMapping
    public ApiResponse<List<GroupDetailResponse>> getGroups(@AuthenticationPrincipal UserPrincipal principal) {
        List<GroupDetailResponse> groups = groupService.getUserGroups(principal.getId());
        return ApiResponse.success(groups);
    }

    /**
     * Create a new group
     * POST /groups
     */
    @PostMapping
    public ApiResponse<GroupDetailResponse> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest request) {
        GroupDetailResponse group = groupService.createGroup(principal.getId(), request);
        return ApiResponse.success(group);
    }

    /**
     * Get group details
     * GET /groups/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<GroupDetailResponse> getGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        GroupDetailResponse group = groupService.getGroup(principal.getId(), id);
        return ApiResponse.success(group);
    }

    /**
     * Update group info
     * PUT /groups/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<GroupDetailResponse> updateGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupDetailResponse group = groupService.updateGroup(principal.getId(), id, request);
        return ApiResponse.success(group);
    }

    /**
     * Delete group (owner only)
     * DELETE /groups/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        groupService.deleteGroup(principal.getId(), id);
        return ApiResponse.success();
    }

    /**
     * Get group members
     * GET /groups/{id}/members
     */
    @GetMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> getGroupMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        List<GroupMemberResponse> members = groupService.getGroupMembers(principal.getId(), id);
        return ApiResponse.success(members);
    }

    /**
     * Add members to group
     * POST /groups/{id}/members
     */
    @PostMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> addMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddGroupMembersRequest request) {
        List<GroupMemberResponse> members = groupService.addMembers(principal.getId(), id, request);
        return ApiResponse.success(members);
    }

    /**
     * Remove member from group
     * DELETE /groups/{id}/members/{uid}
     */
    @DeleteMapping("/{id}/members/{uid}")
    public ApiResponse<Void> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long uid) {
        groupService.removeMember(principal.getId(), id, uid);
        return ApiResponse.success();
    }

    /**
     * Transfer group ownership
     * POST /groups/{id}/transfer
     */
    @PostMapping("/{id}/transfer")
    public ApiResponse<GroupDetailResponse> transferOwnership(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TransferOwnershipRequest request) {
        GroupDetailResponse group = groupService.transferOwnership(principal.getId(), id, request.getNewOwnerId());
        return ApiResponse.success(group);
    }

    /**
     * Leave group
     * POST /groups/{id}/leave
     */
    @PostMapping("/{id}/leave")
    public ApiResponse<Void> leaveGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        groupService.leaveGroup(principal.getId(), id);
        return ApiResponse.success();
    }
}
