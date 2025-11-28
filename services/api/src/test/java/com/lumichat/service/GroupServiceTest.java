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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService Tests")
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private User memberUser;
    private Group testGroup;
    private GroupMember ownerMember;
    private GroupMember regularMember;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .nickname("TestUser")
                .status(User.UserStatus.active)
                .build();

        memberUser = User.builder()
                .id(2L)
                .uid("LC87654321")
                .email("member@example.com")
                .nickname("MemberUser")
                .status(User.UserStatus.active)
                .build();

        testGroup = Group.builder()
                .id(100L)
                .gid("G12345678")
                .name("Test Group")
                .owner(testUser)
                .creator(testUser)
                .maxMembers(500)
                .createdAt(LocalDateTime.now())
                .build();

        ownerMember = GroupMember.builder()
                .id(1L)
                .group(testGroup)
                .user(testUser)
                .role(GroupMember.MemberRole.owner)
                .joinedAt(LocalDateTime.now())
                .build();

        regularMember = GroupMember.builder()
                .id(2L)
                .group(testGroup)
                .user(memberUser)
                .role(GroupMember.MemberRole.member)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GetUserGroups Tests")
    class GetUserGroupsTests {

        @Test
        @DisplayName("Should get user groups successfully")
        void shouldGetUserGroupsSuccessfully() {
            // Given
            when(groupMemberRepository.findByUserId(1L))
                    .thenReturn(Arrays.asList(ownerMember));
            when(groupMemberRepository.countByGroupId(100L)).thenReturn(2);

            // When
            List<GroupDetailResponse> results = groupService.getUserGroups(1L);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no groups")
        void shouldReturnEmptyListWhenNoGroups() {
            // Given
            when(groupMemberRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<GroupDetailResponse> results = groupService.getUserGroups(1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("GetGroup Tests")
    class GetGroupTests {

        @Test
        @DisplayName("Should get group successfully")
        void shouldGetGroupSuccessfully() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByGroupIdAndUserId(100L, 1L)).thenReturn(true);
            when(groupMemberRepository.countByGroupId(100L)).thenReturn(2);

            // When
            GroupDetailResponse result = groupService.getGroup(1L, 100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            // Given
            when(groupRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> groupService.getGroup(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Group not found");
        }

        @Test
        @DisplayName("Should throw exception when user not a member")
        void shouldThrowExceptionWhenUserNotMember() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByGroupIdAndUserId(100L, 999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> groupService.getGroup(999L, 100L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You are not a member of this group");
        }
    }

    @Nested
    @DisplayName("CreateGroup Tests")
    class CreateGroupTests {

        @Test
        @DisplayName("Should create group successfully")
        void shouldCreateGroupSuccessfully() {
            // Given
            CreateGroupRequest request = new CreateGroupRequest();
            request.setName("New Group");
            request.setAnnouncement("Welcome!");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(groupRepository.existsByGid(anyString())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
                Group g = inv.getArgument(0);
                g.setId(101L);
                g.setCreatedAt(LocalDateTime.now());
                return g;
            });
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMemberRepository.countByGroupId(101L)).thenReturn(1);

            // When
            GroupDetailResponse result = groupService.createGroup(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("Should create group with initial members")
        void shouldCreateGroupWithInitialMembers() {
            // Given
            CreateGroupRequest request = new CreateGroupRequest();
            request.setName("New Group");
            request.setMemberIds(Arrays.asList(2L, 3L));

            User user3 = User.builder().id(3L).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(groupRepository.existsByGid(anyString())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
                Group g = inv.getArgument(0);
                g.setId(101L);
                g.setCreatedAt(LocalDateTime.now());
                return g;
            });
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMemberRepository.existsByGroupIdAndUserId(eq(101L), anyLong())).thenReturn(false);
            when(groupMemberRepository.countByGroupId(101L)).thenReturn(1).thenReturn(3);
            when(userRepository.findById(2L)).thenReturn(Optional.of(memberUser));
            when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

            // When
            GroupDetailResponse result = groupService.createGroup(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(groupMemberRepository, atLeast(3)).save(any(GroupMember.class)); // Owner + 2 members
        }

        @Test
        @DisplayName("Should throw exception when creator not found")
        void shouldThrowExceptionWhenCreatorNotFound() {
            // Given
            CreateGroupRequest request = new CreateGroupRequest();
            request.setName("New Group");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> groupService.createGroup(999L, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("UpdateGroup Tests")
    class UpdateGroupTests {

        @Test
        @DisplayName("Should update group as owner")
        void shouldUpdateGroupAsOwner() {
            // Given
            UpdateGroupRequest request = new UpdateGroupRequest();
            request.setName("Updated Name");
            request.setAnnouncement("Updated Announcement");

            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMemberRepository.countByGroupId(100L)).thenReturn(2);

            // When
            GroupDetailResponse result = groupService.updateGroup(1L, 100L, request);

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
            verify(groupRepository).save(groupCaptor.capture());

            Group savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getName()).isEqualTo("Updated Name");
            assertThat(savedGroup.getAnnouncement()).isEqualTo("Updated Announcement");
        }

        @Test
        @DisplayName("Should throw exception when member tries to update")
        void shouldThrowExceptionWhenMemberTriesToUpdate() {
            // Given
            UpdateGroupRequest request = new UpdateGroupRequest();
            request.setName("New Name");

            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(regularMember));

            // When/Then
            assertThatThrownBy(() -> groupService.updateGroup(2L, 100L, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only owner or admin can update group info");
        }
    }

    @Nested
    @DisplayName("DeleteGroup Tests")
    class DeleteGroupTests {

        @Test
        @DisplayName("Should delete group as owner")
        void shouldDeleteGroupAsOwner() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

            // When
            groupService.deleteGroup(1L, 100L);

            // Then
            verify(groupMemberRepository).deleteAllByGroupId(100L);
            verify(groupRepository).delete(testGroup);
        }

        @Test
        @DisplayName("Should throw exception when non-owner tries to delete")
        void shouldThrowExceptionWhenNonOwnerTriesToDelete() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

            // When/Then
            assertThatThrownBy(() -> groupService.deleteGroup(2L, 100L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the owner can delete the group");
        }
    }

    @Nested
    @DisplayName("GetGroupMembers Tests")
    class GetGroupMembersTests {

        @Test
        @DisplayName("Should get group members successfully")
        void shouldGetGroupMembersSuccessfully() {
            // Given
            when(groupMemberRepository.existsByGroupIdAndUserId(100L, 1L)).thenReturn(true);
            when(groupMemberRepository.findByGroupId(100L))
                    .thenReturn(Arrays.asList(ownerMember, regularMember));

            // When
            List<GroupMemberResponse> results = groupService.getGroupMembers(1L, 100L);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception when non-member tries to get members")
        void shouldThrowExceptionWhenNonMemberTriesToGetMembers() {
            // Given
            when(groupMemberRepository.existsByGroupIdAndUserId(100L, 999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> groupService.getGroupMembers(999L, 100L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You are not a member of this group");
        }
    }

    @Nested
    @DisplayName("AddMembers Tests")
    class AddMembersTests {

        @Test
        @DisplayName("Should add members as owner")
        void shouldAddMembersAsOwner() {
            // Given
            AddGroupMembersRequest request = new AddGroupMembersRequest();
            request.setMemberIds(Arrays.asList(3L));

            User newUser = User.builder().id(3L).build();

            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.existsByGroupIdAndUserId(100L, 3L)).thenReturn(false);
            when(groupMemberRepository.countByGroupId(100L)).thenReturn(2);
            when(userRepository.findById(3L)).thenReturn(Optional.of(newUser));
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<GroupMemberResponse> results = groupService.addMembers(1L, 100L, request);

            // Then
            assertThat(results).hasSize(1);
            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("Should throw exception when member tries to add members")
        void shouldThrowExceptionWhenMemberTriesToAddMembers() {
            // Given
            AddGroupMembersRequest request = new AddGroupMembersRequest();
            request.setMemberIds(Arrays.asList(3L));

            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(regularMember));

            // When/Then
            assertThatThrownBy(() -> groupService.addMembers(2L, 100L, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only owner or admin can add members");
        }
    }

    @Nested
    @DisplayName("RemoveMember Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member as owner")
        void shouldRemoveMemberAsOwner() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(regularMember));

            // When
            groupService.removeMember(1L, 100L, 2L);

            // Then
            verify(groupMemberRepository).delete(regularMember);
        }

        @Test
        @DisplayName("Should throw exception when trying to remove self")
        void shouldThrowExceptionWhenTryingToRemoveSelf() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(ownerMember));

            // When/Then
            assertThatThrownBy(() -> groupService.removeMember(1L, 100L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Use leave group instead");
        }

        @Test
        @DisplayName("Should throw exception when admin tries to remove owner")
        void shouldThrowExceptionWhenAdminTriesToRemoveOwner() {
            // Given
            GroupMember adminMember = GroupMember.builder()
                    .id(3L)
                    .group(testGroup)
                    .user(memberUser)
                    .role(GroupMember.MemberRole.admin)
                    .build();

            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(adminMember));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(ownerMember));

            // When/Then
            assertThatThrownBy(() -> groupService.removeMember(2L, 100L, 1L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Admin cannot remove owner or other admins");
        }
    }

    @Nested
    @DisplayName("TransferOwnership Tests")
    class TransferOwnershipTests {

        @Test
        @DisplayName("Should transfer ownership successfully")
        void shouldTransferOwnershipSuccessfully() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(regularMember));
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMemberRepository.countByGroupId(100L)).thenReturn(2);

            // When
            GroupDetailResponse result = groupService.transferOwnership(1L, 100L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(groupMemberRepository).updateRole(100L, 1L, GroupMember.MemberRole.admin);
            verify(groupMemberRepository).updateRole(100L, 2L, GroupMember.MemberRole.owner);
        }

        @Test
        @DisplayName("Should throw exception when non-owner tries to transfer")
        void shouldThrowExceptionWhenNonOwnerTriesToTransfer() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

            // When/Then
            assertThatThrownBy(() -> groupService.transferOwnership(2L, 100L, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the owner can transfer ownership");
        }

        @Test
        @DisplayName("Should throw exception when new owner not a member")
        void shouldThrowExceptionWhenNewOwnerNotMember() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 999L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> groupService.transferOwnership(1L, 100L, 999L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("New owner must be a member of the group");
        }
    }

    @Nested
    @DisplayName("LeaveGroup Tests")
    class LeaveGroupTests {

        @Test
        @DisplayName("Should leave group as member")
        void shouldLeaveGroupAsMember() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(regularMember));

            // When
            groupService.leaveGroup(2L, 100L);

            // Then
            verify(groupMemberRepository).delete(regularMember);
        }

        @Test
        @DisplayName("Should throw exception when owner tries to leave")
        void shouldThrowExceptionWhenOwnerTriesToLeave() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(ownerMember));

            // When/Then
            assertThatThrownBy(() -> groupService.leaveGroup(1L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Owner cannot leave the group. Transfer ownership first or delete the group.");
        }

        @Test
        @DisplayName("Should throw exception when not a member")
        void shouldThrowExceptionWhenNotMember() {
            // Given
            when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 999L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> groupService.leaveGroup(999L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You are not a member of this group");
        }
    }
}
