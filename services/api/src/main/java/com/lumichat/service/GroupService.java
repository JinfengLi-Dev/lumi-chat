package com.lumichat.service;

import com.lumichat.dto.request.AddGroupMembersRequest;
import com.lumichat.dto.request.CreateGroupRequest;
import com.lumichat.dto.request.UpdateGroupRequest;
import com.lumichat.dto.response.GroupDetailResponse;
import com.lumichat.dto.response.GroupMemberResponse;
import com.lumichat.entity.Group;
import com.lumichat.entity.GroupMember;
import com.lumichat.entity.User;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.ForbiddenException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.GroupMemberRepository;
import com.lumichat.repository.GroupRepository;
import com.lumichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    /**
     * Get all groups for a user
     */
    public List<GroupDetailResponse> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(gm -> GroupDetailResponse.from(gm.getGroup()))
                .collect(Collectors.toList());
    }

    /**
     * Get group details
     */
    public GroupDetailResponse getGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // Check if user is a member
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("You are not a member of this group");
        }

        return GroupDetailResponse.from(group);
    }

    /**
     * Create a new group
     */
    @Transactional
    public GroupDetailResponse createGroup(Long userId, CreateGroupRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Generate unique GID
        String gid = generateUniqueGid();

        Group group = Group.builder()
                .gid(gid)
                .name(request.getName())
                .avatar(request.getAvatar())
                .announcement(request.getAnnouncement())
                .owner(creator)
                .creator(creator)
                .memberCount(1)
                .build();

        group = groupRepository.save(group);

        // Add creator as owner
        GroupMember ownerMember = GroupMember.builder()
                .group(group)
                .user(creator)
                .role(GroupMember.MemberRole.owner)
                .build();
        groupMemberRepository.save(ownerMember);

        // Add initial members if provided
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<Long> validMemberIds = new ArrayList<>();
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    validMemberIds.add(memberId);
                }
            }
            addMembersInternal(group, creator, validMemberIds);
        }

        log.info("Group {} created by user {}", group.getId(), userId);
        return GroupDetailResponse.from(group);
    }

    /**
     * Update group info
     */
    @Transactional
    public GroupDetailResponse updateGroup(Long userId, Long groupId, UpdateGroupRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // Check if user is owner or admin
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        if (member.getRole() == GroupMember.MemberRole.member) {
            throw new ForbiddenException("Only owner or admin can update group info");
        }

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            group.setAvatar(request.getAvatar());
        }
        if (request.getAnnouncement() != null) {
            group.setAnnouncement(request.getAnnouncement());
        }

        group = groupRepository.save(group);
        log.info("Group {} updated by user {}", groupId, userId);
        return GroupDetailResponse.from(group);
    }

    /**
     * Delete group (owner only)
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!group.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the owner can delete the group");
        }

        // Delete all members first
        groupMemberRepository.deleteAllByGroupId(groupId);

        // Delete the group
        groupRepository.delete(group);
        log.info("Group {} deleted by owner {}", groupId, userId);
    }

    /**
     * Get group members
     */
    public List<GroupMemberResponse> getGroupMembers(Long userId, Long groupId) {
        // Check if user is a member
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ForbiddenException("You are not a member of this group");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return members.stream()
                .map(GroupMemberResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Add members to group
     */
    @Transactional
    public List<GroupMemberResponse> addMembers(Long userId, Long groupId, AddGroupMembersRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        GroupMember inviter = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        // Only owner/admin can add members, or if the group allows member invitation
        if (inviter.getRole() == GroupMember.MemberRole.member) {
            throw new ForbiddenException("Only owner or admin can add members");
        }

        User inviterUser = inviter.getUser();
        List<GroupMember> addedMembers = addMembersInternal(group, inviterUser, request.getMemberIds());

        log.info("User {} added {} members to group {}", userId, addedMembers.size(), groupId);
        return addedMembers.stream()
                .map(GroupMemberResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Remove member from group
     */
    @Transactional
    public void removeMember(Long userId, Long groupId, Long targetUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        GroupMember actor = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));

        if (userId.equals(targetUserId)) {
            throw new BadRequestException("Use leave group instead");
        }

        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Target user is not a member of this group"));

        // Owner can remove anyone, admin can remove members only
        if (actor.getRole() == GroupMember.MemberRole.member) {
            throw new ForbiddenException("Only owner or admin can remove members");
        }

        if (actor.getRole() == GroupMember.MemberRole.admin &&
            target.getRole() != GroupMember.MemberRole.member) {
            throw new ForbiddenException("Admin cannot remove owner or other admins");
        }

        groupMemberRepository.delete(target);

        // Update member count
        group.setMemberCount(group.getMemberCount() - 1);
        groupRepository.save(group);

        log.info("User {} removed user {} from group {}", userId, targetUserId, groupId);
    }

    /**
     * Transfer group ownership
     */
    @Transactional
    public GroupDetailResponse transferOwnership(Long userId, Long groupId, Long newOwnerId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!group.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the owner can transfer ownership");
        }

        GroupMember newOwnerMember = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId)
                .orElseThrow(() -> new BadRequestException("New owner must be a member of the group"));

        User newOwner = newOwnerMember.getUser();

        // Update the old owner to admin
        groupMemberRepository.updateRole(groupId, userId, GroupMember.MemberRole.admin);

        // Update the new owner
        groupMemberRepository.updateRole(groupId, newOwnerId, GroupMember.MemberRole.owner);

        // Update group owner
        group.setOwner(newOwner);
        group = groupRepository.save(group);

        log.info("Group {} ownership transferred from {} to {}", groupId, userId, newOwnerId);
        return GroupDetailResponse.from(group);
    }

    /**
     * Leave group
     */
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this group"));

        if (member.getRole() == GroupMember.MemberRole.owner) {
            throw new BadRequestException("Owner cannot leave the group. Transfer ownership first or delete the group.");
        }

        groupMemberRepository.delete(member);

        // Update member count
        group.setMemberCount(group.getMemberCount() - 1);
        groupRepository.save(group);

        log.info("User {} left group {}", userId, groupId);
    }

    // Helper methods

    private String generateUniqueGid() {
        String gid;
        do {
            gid = "G" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (groupRepository.existsByGid(gid));
        return gid;
    }

    private List<GroupMember> addMembersInternal(Group group, User inviter, List<Long> memberIds) {
        List<GroupMember> addedMembers = new ArrayList<>();

        for (Long memberId : memberIds) {
            // Skip if already a member
            if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), memberId)) {
                continue;
            }

            // Check group capacity
            if (group.getMemberCount() >= group.getMaxMembers()) {
                log.warn("Group {} is full, cannot add more members", group.getId());
                break;
            }

            User member = userRepository.findById(memberId).orElse(null);
            if (member == null) {
                continue;
            }

            GroupMember groupMember = GroupMember.builder()
                    .group(group)
                    .user(member)
                    .role(GroupMember.MemberRole.member)
                    .invitedBy(inviter)
                    .build();
            groupMember = groupMemberRepository.save(groupMember);
            addedMembers.add(groupMember);

            // Update member count
            group.setMemberCount(group.getMemberCount() + 1);
        }

        if (!addedMembers.isEmpty()) {
            groupRepository.save(group);
        }

        return addedMembers;
    }
}
