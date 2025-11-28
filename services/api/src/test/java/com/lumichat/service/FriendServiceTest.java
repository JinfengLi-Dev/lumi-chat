package com.lumichat.service;

import com.lumichat.dto.request.SendFriendRequestRequest;
import com.lumichat.dto.response.FriendRequestResponse;
import com.lumichat.dto.response.FriendResponse;
import com.lumichat.entity.FriendRequest;
import com.lumichat.entity.Friendship;
import com.lumichat.entity.User;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.FriendRequestRepository;
import com.lumichat.repository.FriendshipRepository;
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
@DisplayName("FriendService Tests")
class FriendServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendService friendService;

    private User testUser;
    private User friendUser;
    private Friendship friendship;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .nickname("TestUser")
                .status(User.UserStatus.active)
                .build();

        friendUser = User.builder()
                .id(2L)
                .uid("LC87654321")
                .email("friend@example.com")
                .nickname("FriendUser")
                .status(User.UserStatus.active)
                .build();

        friendship = Friendship.builder()
                .id(1L)
                .user(testUser)
                .friend(friendUser)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        friendRequest = FriendRequest.builder()
                .id(100L)
                .fromUser(friendUser)
                .toUser(testUser)
                .message("Hi, let's be friends!")
                .status(FriendRequest.RequestStatus.pending)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GetFriends Tests")
    class GetFriendsTests {

        @Test
        @DisplayName("Should get friends list successfully")
        void shouldGetFriendsListSuccessfully() {
            // Given
            when(friendshipRepository.findByUserIdAndActive(1L))
                    .thenReturn(Arrays.asList(friendship));

            // When
            List<FriendResponse> results = friendService.getFriends(1L);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no friends")
        void shouldReturnEmptyListWhenNoFriends() {
            // Given
            when(friendshipRepository.findByUserIdAndActive(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<FriendResponse> results = friendService.getFriends(1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("GetAllFriends Tests")
    class GetAllFriendsTests {

        @Test
        @DisplayName("Should get all friends including blocked")
        void shouldGetAllFriendsIncludingBlocked() {
            // Given
            Friendship blockedFriendship = Friendship.builder()
                    .id(2L)
                    .user(testUser)
                    .friend(friendUser)
                    .status("blocked")
                    .build();

            when(friendshipRepository.findByUserId(1L))
                    .thenReturn(Arrays.asList(friendship, blockedFriendship));

            // When
            List<FriendResponse> results = friendService.getAllFriends(1L);

            // Then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("SendFriendRequest Tests")
    class SendFriendRequestTests {

        @Test
        @DisplayName("Should send friend request successfully")
        void shouldSendFriendRequestSuccessfully() {
            // Given
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid("LC87654321");
            request.setMessage("Hi, let's be friends!");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByUid("LC87654321")).thenReturn(Optional.of(friendUser));
            when(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).thenReturn(false);
            when(friendRequestRepository.existsPendingRequest(1L, 2L)).thenReturn(false);
            when(friendRequestRepository.findPendingRequest(2L, 1L)).thenReturn(Optional.empty());
            when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(inv -> {
                FriendRequest fr = inv.getArgument(0);
                fr.setId(101L);
                fr.setCreatedAt(LocalDateTime.now());
                return fr;
            });

            // When
            FriendRequestResponse result = friendService.sendFriendRequest(1L, request);

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<FriendRequest> requestCaptor = ArgumentCaptor.forClass(FriendRequest.class);
            verify(friendRequestRepository).save(requestCaptor.capture());

            FriendRequest savedRequest = requestCaptor.getValue();
            assertThat(savedRequest.getFromUser().getId()).isEqualTo(1L);
            assertThat(savedRequest.getToUser().getId()).isEqualTo(2L);
            assertThat(savedRequest.getMessage()).isEqualTo("Hi, let's be friends!");
        }

        @Test
        @DisplayName("Should throw exception when sending request to self")
        void shouldThrowExceptionWhenSendingToSelf() {
            // Given
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid("LC12345678");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByUid("LC12345678")).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot send friend request to yourself");
        }

        @Test
        @DisplayName("Should throw exception when already friends")
        void shouldThrowExceptionWhenAlreadyFriends() {
            // Given
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid("LC87654321");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByUid("LC87654321")).thenReturn(Optional.of(friendUser));
            when(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Already friends with this user");
        }

        @Test
        @DisplayName("Should throw exception when pending request exists")
        void shouldThrowExceptionWhenPendingRequestExists() {
            // Given
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid("LC87654321");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByUid("LC87654321")).thenReturn(Optional.of(friendUser));
            when(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).thenReturn(false);
            when(friendRequestRepository.existsPendingRequest(1L, 2L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Friend request already sent");
        }

        @Test
        @DisplayName("Should auto-accept when reverse request exists")
        void shouldAutoAcceptWhenReverseRequestExists() {
            // Given
            SendFriendRequestRequest request = new SendFriendRequestRequest();
            request.setUid("LC87654321");

            FriendRequest reverseRequest = FriendRequest.builder()
                    .id(50L)
                    .fromUser(friendUser)
                    .toUser(testUser)
                    .status(FriendRequest.RequestStatus.pending)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByUid("LC87654321")).thenReturn(Optional.of(friendUser));
            when(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).thenReturn(false);
            when(friendRequestRepository.existsPendingRequest(1L, 2L)).thenReturn(false);
            when(friendRequestRepository.findPendingRequest(2L, 1L)).thenReturn(Optional.of(reverseRequest));
            when(friendRequestRepository.findById(50L)).thenReturn(Optional.of(reverseRequest));
            when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(reverseRequest);
            when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            FriendRequestResponse result = friendService.sendFriendRequest(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(friendshipRepository, times(2)).save(any(Friendship.class)); // Bidirectional friendship
        }
    }

    @Nested
    @DisplayName("GetFriendRequests Tests")
    class GetFriendRequestsTests {

        @Test
        @DisplayName("Should get all friend requests")
        void shouldGetAllFriendRequests() {
            // Given
            when(friendRequestRepository.findByToUserId(1L))
                    .thenReturn(Arrays.asList(friendRequest));

            // When
            List<FriendRequestResponse> results = friendService.getFriendRequests(1L);

            // Then
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GetPendingFriendRequests Tests")
    class GetPendingFriendRequestsTests {

        @Test
        @DisplayName("Should get pending friend requests")
        void shouldGetPendingFriendRequests() {
            // Given
            when(friendRequestRepository.findPendingByToUserId(1L))
                    .thenReturn(Arrays.asList(friendRequest));

            // When
            List<FriendRequestResponse> results = friendService.getPendingFriendRequests(1L);

            // Then
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("AcceptFriendRequest Tests")
    class AcceptFriendRequestTests {

        @Test
        @DisplayName("Should accept friend request successfully")
        void shouldAcceptFriendRequestSuccessfully() {
            // Given
            when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(friendRequest));
            when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(friendRequest);
            when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            FriendRequestResponse result = friendService.acceptFriendRequest(1L, 100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(friendRequest.getStatus()).isEqualTo(FriendRequest.RequestStatus.accepted);
            assertThat(friendRequest.getHandledAt()).isNotNull();

            verify(friendshipRepository, times(2)).save(any(Friendship.class));
        }

        @Test
        @DisplayName("Should throw exception when request not found")
        void shouldThrowExceptionWhenRequestNotFound() {
            // Given
            when(friendRequestRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Friend request not found");
        }

        @Test
        @DisplayName("Should throw exception when accepting others request")
        void shouldThrowExceptionWhenAcceptingOthersRequest() {
            // Given
            friendRequest.setToUser(friendUser); // Request is for different user
            when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(friendRequest));

            // When/Then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(1L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This friend request is not for you");
        }

        @Test
        @DisplayName("Should throw exception when request already handled")
        void shouldThrowExceptionWhenRequestAlreadyHandled() {
            // Given
            friendRequest.setStatus(FriendRequest.RequestStatus.accepted);
            when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(friendRequest));

            // When/Then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(1L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Friend request already handled");
        }
    }

    @Nested
    @DisplayName("RejectFriendRequest Tests")
    class RejectFriendRequestTests {

        @Test
        @DisplayName("Should reject friend request successfully")
        void shouldRejectFriendRequestSuccessfully() {
            // Given
            when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(friendRequest));
            when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(friendRequest);

            // When
            FriendRequestResponse result = friendService.rejectFriendRequest(1L, 100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(friendRequest.getStatus()).isEqualTo(FriendRequest.RequestStatus.rejected);
            assertThat(friendRequest.getHandledAt()).isNotNull();

            verify(friendshipRepository, never()).save(any(Friendship.class));
        }

        @Test
        @DisplayName("Should throw exception when rejecting already handled request")
        void shouldThrowExceptionWhenRejectingAlreadyHandledRequest() {
            // Given
            friendRequest.setStatus(FriendRequest.RequestStatus.rejected);
            when(friendRequestRepository.findById(100L)).thenReturn(Optional.of(friendRequest));

            // When/Then
            assertThatThrownBy(() -> friendService.rejectFriendRequest(1L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Friend request already handled");
        }
    }

    @Nested
    @DisplayName("DeleteFriend Tests")
    class DeleteFriendTests {

        @Test
        @DisplayName("Should delete friend (unfriend) successfully")
        void shouldDeleteFriendSuccessfully() {
            // Given
            when(friendshipRepository.deleteByUserIdAndFriendId(1L, 2L)).thenReturn(1);
            when(friendshipRepository.deleteByUserIdAndFriendId(2L, 1L)).thenReturn(1);

            // When
            friendService.deleteFriend(1L, 2L);

            // Then
            verify(friendshipRepository).deleteByUserIdAndFriendId(1L, 2L);
            verify(friendshipRepository).deleteByUserIdAndFriendId(2L, 1L);
        }

        @Test
        @DisplayName("Should throw exception when friendship not found")
        void shouldThrowExceptionWhenFriendshipNotFound() {
            // Given
            when(friendshipRepository.deleteByUserIdAndFriendId(1L, 999L)).thenReturn(0);
            when(friendshipRepository.deleteByUserIdAndFriendId(999L, 1L)).thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> friendService.deleteFriend(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Friendship not found");
        }
    }

    @Nested
    @DisplayName("UpdateRemark Tests")
    class UpdateRemarkTests {

        @Test
        @DisplayName("Should update remark successfully")
        void shouldUpdateRemarkSuccessfully() {
            // Given
            when(friendshipRepository.updateRemark(1L, 2L, "BestFriend")).thenReturn(1);

            // When
            friendService.updateRemark(1L, 2L, "BestFriend");

            // Then
            verify(friendshipRepository).updateRemark(1L, 2L, "BestFriend");
        }

        @Test
        @DisplayName("Should throw exception when updating remark for non-friend")
        void shouldThrowExceptionWhenUpdatingRemarkForNonFriend() {
            // Given
            when(friendshipRepository.updateRemark(1L, 999L, "Remark")).thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> friendService.updateRemark(1L, 999L, "Remark"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Friendship not found");
        }
    }

    @Nested
    @DisplayName("BlockFriend Tests")
    class BlockFriendTests {

        @Test
        @DisplayName("Should block friend successfully")
        void shouldBlockFriendSuccessfully() {
            // Given
            when(friendshipRepository.updateStatus(1L, 2L, "blocked")).thenReturn(1);

            // When
            friendService.blockFriend(1L, 2L);

            // Then
            verify(friendshipRepository).updateStatus(1L, 2L, "blocked");
        }

        @Test
        @DisplayName("Should throw exception when blocking non-friend")
        void shouldThrowExceptionWhenBlockingNonFriend() {
            // Given
            when(friendshipRepository.updateStatus(1L, 999L, "blocked")).thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> friendService.blockFriend(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Friendship not found");
        }
    }

    @Nested
    @DisplayName("UnblockFriend Tests")
    class UnblockFriendTests {

        @Test
        @DisplayName("Should unblock friend successfully")
        void shouldUnblockFriendSuccessfully() {
            // Given
            when(friendshipRepository.updateStatus(1L, 2L, "active")).thenReturn(1);

            // When
            friendService.unblockFriend(1L, 2L);

            // Then
            verify(friendshipRepository).updateStatus(1L, 2L, "active");
        }

        @Test
        @DisplayName("Should throw exception when unblocking non-friend")
        void shouldThrowExceptionWhenUnblockingNonFriend() {
            // Given
            when(friendshipRepository.updateStatus(1L, 999L, "active")).thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> friendService.unblockFriend(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Friendship not found");
        }
    }
}
