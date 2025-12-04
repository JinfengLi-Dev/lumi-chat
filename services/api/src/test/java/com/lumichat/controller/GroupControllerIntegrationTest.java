package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.AddGroupMembersRequest;
import com.lumichat.dto.request.CreateGroupRequest;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.TransferOwnershipRequest;
import com.lumichat.dto.request.UpdateGroupRequest;
import com.lumichat.entity.Group;
import com.lumichat.entity.GroupMember;
import com.lumichat.entity.User;
import com.lumichat.repository.GroupMemberRepository;
import com.lumichat.repository.GroupRepository;
import com.lumichat.repository.UserDeviceRepository;
import com.lumichat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private User user3;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");
        user3 = createUser("USER003", "user3@example.com", "User Three");

        user1Token = loginAndGetToken("user1@example.com");
        user2Token = loginAndGetToken("user2@example.com");
    }

    private User createUser(String uid, String email, String nickname) {
        User user = User.builder()
                .uid(uid)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname(nickname)
                .gender(User.Gender.male)
                .status(User.UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setDeviceId("device-" + email);
        request.setDeviceType("web");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    @Nested
    @DisplayName("GET /groups")
    class GetGroupsTests {

        @Test
        @DisplayName("Should return empty list when user has no groups")
        void shouldReturnEmptyListWhenNoGroups() throws Exception {
            mockMvc.perform(get("/groups")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return user's groups")
        void shouldReturnUserGroups() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            mockMvc.perform(get("/groups")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("Test Group"))
                    .andExpect(jsonPath("$.data[0].ownerId").value(user1.getId()));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/groups"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /groups")
    class CreateGroupTests {

        @Test
        @DisplayName("Should create group successfully")
        void shouldCreateGroupSuccessfully() throws Exception {
            CreateGroupRequest request = new CreateGroupRequest();
            request.setName("My New Group");
            request.setAnnouncement("Welcome to the group!");

            mockMvc.perform(post("/groups")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("My New Group"))
                    .andExpect(jsonPath("$.data.announcement").value("Welcome to the group!"))
                    .andExpect(jsonPath("$.data.ownerId").value(user1.getId()))
                    .andExpect(jsonPath("$.data.memberCount").value(1));
        }

        @Test
        @DisplayName("Should create group with initial members")
        void shouldCreateGroupWithMembers() throws Exception {
            CreateGroupRequest request = new CreateGroupRequest();
            request.setName("Group With Members");
            request.setMemberIds(List.of(user2.getId(), user3.getId()));

            mockMvc.perform(post("/groups")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("Group With Members"))
                    .andExpect(jsonPath("$.data.memberCount").value(3));
        }

        @Test
        @DisplayName("Should fail when group name is empty")
        void shouldFailWhenNameEmpty() throws Exception {
            CreateGroupRequest request = new CreateGroupRequest();
            request.setName("");

            mockMvc.perform(post("/groups")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /groups/{id}")
    class GetGroupTests {

        @Test
        @DisplayName("Should get group details")
        void shouldGetGroupDetails() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            mockMvc.perform(get("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(group.getId()))
                    .andExpect(jsonPath("$.data.name").value("Test Group"));
        }

        @Test
        @DisplayName("Should fail when group not found")
        void shouldFailWhenGroupNotFound() throws Exception {
            mockMvc.perform(get("/groups/99999")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail when not a member")
        void shouldFailWhenNotMember() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            mockMvc.perform(get("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /groups/{id}")
    class UpdateGroupTests {

        @Test
        @DisplayName("Should update group as owner")
        void shouldUpdateGroupAsOwner() throws Exception {
            Group group = createGroup("Original Name", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            UpdateGroupRequest request = new UpdateGroupRequest();
            request.setName("Updated Name");
            request.setAnnouncement("New announcement");

            mockMvc.perform(put("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("Updated Name"))
                    .andExpect(jsonPath("$.data.announcement").value("New announcement"));
        }

        @Test
        @DisplayName("Should update group as admin")
        void shouldUpdateGroupAsAdmin() throws Exception {
            Group group = createGroup("Original Name", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.admin);

            UpdateGroupRequest request = new UpdateGroupRequest();
            request.setName("Admin Updated");

            mockMvc.perform(put("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("Admin Updated"));
        }

        @Test
        @DisplayName("Should fail when regular member tries to update")
        void shouldFailWhenMemberTriesToUpdate() throws Exception {
            Group group = createGroup("Original Name", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            UpdateGroupRequest request = new UpdateGroupRequest();
            request.setName("Member Updated");

            mockMvc.perform(put("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /groups/{id}")
    class DeleteGroupTests {

        @Test
        @DisplayName("Should delete group as owner")
        void shouldDeleteGroupAsOwner() throws Exception {
            Group group = createGroup("To Be Deleted", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            mockMvc.perform(delete("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify group was deleted
            mockMvc.perform(get("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail when admin tries to delete")
        void shouldFailWhenAdminTriesToDelete() throws Exception {
            Group group = createGroup("Admin Cannot Delete", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.admin);

            mockMvc.perform(delete("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /groups/{id}/members")
    class GetGroupMembersTests {

        @Test
        @DisplayName("Should get group members")
        void shouldGetGroupMembers() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            mockMvc.perform(get("/groups/" + group.getId() + "/members")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Should fail when not a member")
        void shouldFailWhenNotMember() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            mockMvc.perform(get("/groups/" + group.getId() + "/members")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /groups/{id}/members")
    class AddMembersTests {

        @Test
        @DisplayName("Should add members as owner")
        void shouldAddMembersAsOwner() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            AddGroupMembersRequest request = new AddGroupMembersRequest();
            request.setMemberIds(List.of(user2.getId(), user3.getId()));

            mockMvc.perform(post("/groups/" + group.getId() + "/members")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Should add members as admin")
        void shouldAddMembersAsAdmin() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.admin);

            AddGroupMembersRequest request = new AddGroupMembersRequest();
            request.setMemberIds(List.of(user3.getId()));

            mockMvc.perform(post("/groups/" + group.getId() + "/members")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail when regular member tries to add")
        void shouldFailWhenMemberTriesToAdd() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            AddGroupMembersRequest request = new AddGroupMembersRequest();
            request.setMemberIds(List.of(user3.getId()));

            mockMvc.perform(post("/groups/" + group.getId() + "/members")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /groups/{id}/members/{uid}")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member as owner")
        void shouldRemoveMemberAsOwner() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            mockMvc.perform(delete("/groups/" + group.getId() + "/members/" + user2.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify member was removed
            mockMvc.perform(get("/groups/" + group.getId() + "/members")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Should fail when removing owner")
        void shouldFailWhenRemovingOwner() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.admin);

            mockMvc.perform(delete("/groups/" + group.getId() + "/members/" + user1.getId())
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /groups/{id}/transfer")
    class TransferOwnershipTests {

        @Test
        @DisplayName("Should transfer ownership successfully")
        void shouldTransferOwnershipSuccessfully() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            TransferOwnershipRequest request = new TransferOwnershipRequest();
            request.setNewOwnerId(user2.getId());

            mockMvc.perform(post("/groups/" + group.getId() + "/transfer")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.ownerId").value(user2.getId()));
        }

        @Test
        @DisplayName("Should fail when non-owner tries to transfer")
        void shouldFailWhenNonOwnerTriesToTransfer() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.admin);

            TransferOwnershipRequest request = new TransferOwnershipRequest();
            request.setNewOwnerId(user2.getId());

            mockMvc.perform(post("/groups/" + group.getId() + "/transfer")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail when transferring to non-member")
        void shouldFailWhenTransferringToNonMember() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);

            TransferOwnershipRequest request = new TransferOwnershipRequest();
            request.setNewOwnerId(user2.getId());

            mockMvc.perform(post("/groups/" + group.getId() + "/transfer")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /groups/{id}/leave")
    class LeaveGroupTests {

        @Test
        @DisplayName("Should leave group successfully")
        void shouldLeaveGroupSuccessfully() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            mockMvc.perform(post("/groups/" + group.getId() + "/leave")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify user left the group
            mockMvc.perform(get("/groups/" + group.getId())
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail when owner tries to leave without transfer")
        void shouldFailWhenOwnerTriesToLeave() throws Exception {
            Group group = createGroup("Test Group", user1);
            addMemberToGroup(group, user1, GroupMember.MemberRole.owner);
            addMemberToGroup(group, user2, GroupMember.MemberRole.member);

            mockMvc.perform(post("/groups/" + group.getId() + "/leave")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isBadRequest());
        }
    }

    private Group createGroup(String name, User owner) {
        Group group = Group.builder()
                .gid("GRP" + System.currentTimeMillis())
                .name(name)
                .owner(owner)
                .creator(owner)
                .maxMembers(500)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return groupRepository.save(group);
    }

    private GroupMember addMemberToGroup(Group group, User user, GroupMember.MemberRole role) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
        return groupMemberRepository.save(member);
    }
}
