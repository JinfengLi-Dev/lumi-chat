package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.SendFriendRequestRequest;
import com.lumichat.dto.request.UpdateRemarkRequest;
import com.lumichat.entity.FriendRequest;
import com.lumichat.entity.Friendship;
import com.lumichat.entity.User;
import com.lumichat.repository.FriendRequestRepository;
import com.lumichat.repository.FriendshipRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FriendControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private User user3;
    private String user1Token;

    @BeforeEach
    void setUp() throws Exception {
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");
        user3 = createUser("USER003", "user3@example.com", "User Three");

        user1Token = loginAndGetToken("user1@example.com");
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
    @DisplayName("GET /friends")
    class GetFriendsTests {

        @Test
        @DisplayName("Should return empty list when user has no friends")
        void shouldReturnEmptyListWhenNoFriends() throws Exception {
            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return friends list")
        void shouldReturnFriendsList() throws Exception {
            // Create friendship between user1 and user2
            createFriendship(user1, user2, "active");

            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].uid").value("USER002"))
                    .andExpect(jsonPath("$.data[0].nickname").value("User Two"));
        }

        @Test
        @DisplayName("Should exclude blocked friends by default")
        void shouldExcludeBlockedFriends() throws Exception {
            createFriendship(user1, user2, "blocked");

            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should include blocked friends when requested")
        void shouldIncludeBlockedFriendsWhenRequested() throws Exception {
            createFriendship(user1, user2, "blocked");

            mockMvc.perform(get("/friends")
                            .param("includeBlocked", "true")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/friends"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /friends/request")
    class SendFriendRequestTests {

        @Test
        @DisplayName("Should send friend request successfully")
        void shouldSendFriendRequestSuccessfully() throws Exception {
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid(user2.getUid());
            request.setMessage("Hi, let's be friends!");

            mockMvc.perform(post("/friends/request")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fromUser.id").value(user1.getId()))
                    .andExpect(jsonPath("$.data.message").value("Hi, let's be friends!"))
                    .andExpect(jsonPath("$.data.status").value("pending"));
        }

        @Test
        @DisplayName("Should fail when sending request to self")
        void shouldFailWhenSendingToSelf() throws Exception {
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid(user1.getUid());

            mockMvc.perform(post("/friends/request")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail when already friends")
        void shouldFailWhenAlreadyFriends() throws Exception {
            createFriendship(user1, user2, "active");

            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid(user2.getUid());

            mockMvc.perform(post("/friends/request")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() throws Exception {
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid("NONEXISTENT");

            mockMvc.perform(post("/friends/request")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /friends/requests")
    class GetFriendRequestsTests {

        @Test
        @DisplayName("Should return pending friend requests")
        void shouldReturnPendingRequests() throws Exception {
            createFriendRequest(user2, user1, "Hello!");

            mockMvc.perform(get("/friends/requests")
                            .param("pendingOnly", "true")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].fromUser.id").value(user2.getId()));
        }

        @Test
        @DisplayName("Should return all friend requests")
        void shouldReturnAllRequests() throws Exception {
            FriendRequest request = createFriendRequest(user2, user1, "Hello!");
            request.setStatus(FriendRequest.RequestStatus.accepted);
            friendRequestRepository.save(request);

            createFriendRequest(user3, user1, "Hi there!");

            mockMvc.perform(get("/friends/requests")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("POST /friends/requests/{id}/accept")
    class AcceptFriendRequestTests {

        @Test
        @DisplayName("Should accept friend request successfully")
        void shouldAcceptRequestSuccessfully() throws Exception {
            FriendRequest request = createFriendRequest(user2, user1, "Let's connect!");

            mockMvc.perform(post("/friends/requests/" + request.getId() + "/accept")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("accepted"));

            // Verify friendship was created
            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Should fail when accepting non-existent request")
        void shouldFailWhenRequestNotFound() throws Exception {
            mockMvc.perform(post("/friends/requests/99999/accept")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail when accepting own request")
        void shouldFailWhenAcceptingOwnRequest() throws Exception {
            FriendRequest request = createFriendRequest(user1, user2, "Hi!");

            mockMvc.perform(post("/friends/requests/" + request.getId() + "/accept")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /friends/requests/{id}/reject")
    class RejectFriendRequestTests {

        @Test
        @DisplayName("Should reject friend request successfully")
        void shouldRejectRequestSuccessfully() throws Exception {
            FriendRequest request = createFriendRequest(user2, user1, "Be my friend!");

            mockMvc.perform(post("/friends/requests/" + request.getId() + "/reject")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("rejected"));
        }
    }

    @Nested
    @DisplayName("DELETE /friends/{id}")
    class DeleteFriendTests {

        @Test
        @DisplayName("Should delete friend successfully")
        void shouldDeleteFriendSuccessfully() throws Exception {
            createFriendship(user1, user2, "active");

            mockMvc.perform(delete("/friends/" + user2.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify friendship was removed
            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should fail when not friends")
        void shouldFailWhenNotFriends() throws Exception {
            mockMvc.perform(delete("/friends/" + user2.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /friends/{id}/remark")
    class UpdateRemarkTests {

        @Test
        @DisplayName("Should update friend remark successfully")
        void shouldUpdateRemarkSuccessfully() throws Exception {
            createFriendship(user1, user2, "active");

            UpdateRemarkRequest request = new UpdateRemarkRequest();
            request.setRemark("Best Friend");

            mockMvc.perform(put("/friends/" + user2.getId() + "/remark")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should fail when not friends")
        void shouldFailWhenNotFriends() throws Exception {
            UpdateRemarkRequest request = new UpdateRemarkRequest();
            request.setRemark("Nickname");

            mockMvc.perform(put("/friends/" + user2.getId() + "/remark")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /friends/{id}/block")
    class BlockFriendTests {

        @Test
        @DisplayName("Should block friend successfully")
        void shouldBlockFriendSuccessfully() throws Exception {
            createFriendship(user1, user2, "active");

            mockMvc.perform(post("/friends/" + user2.getId() + "/block")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify friend is blocked (not in default list)
            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should fail when not friends")
        void shouldFailWhenNotFriends() throws Exception {
            mockMvc.perform(post("/friends/" + user2.getId() + "/block")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /friends/{id}/unblock")
    class UnblockFriendTests {

        @Test
        @DisplayName("Should unblock friend successfully")
        void shouldUnblockFriendSuccessfully() throws Exception {
            createFriendship(user1, user2, "blocked");

            mockMvc.perform(post("/friends/" + user2.getId() + "/unblock")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify friend is unblocked
            mockMvc.perform(get("/friends")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    private Friendship createFriendship(User user, User friend, String status) {
        Friendship friendship = Friendship.builder()
                .user(user)
                .friend(friend)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        return friendshipRepository.save(friendship);
    }

    private FriendRequest createFriendRequest(User from, User to, String message) {
        FriendRequest request = FriendRequest.builder()
                .fromUser(from)
                .toUser(to)
                .message(message)
                .status(FriendRequest.RequestStatus.pending)
                .createdAt(LocalDateTime.now())
                .build();
        return friendRequestRepository.save(request);
    }
}
