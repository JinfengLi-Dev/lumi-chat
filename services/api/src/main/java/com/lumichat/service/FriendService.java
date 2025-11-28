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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    /**
     * Get all friends for a user
     */
    public List<FriendResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndNotBlocked(userId);
        return friendships.stream()
                .map(FriendResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get all friends including blocked
     */
    public List<FriendResponse> getAllFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserId(userId);
        return friendships.stream()
                .map(FriendResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Send a friend request
     */
    @Transactional
    public FriendRequestResponse sendFriendRequest(Long userId, SendFriendRequestRequest request) {
        User fromUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User toUser = userRepository.findByUid(request.getUid())
                .orElseThrow(() -> new NotFoundException("User with UID " + request.getUid() + " not found"));

        if (userId.equals(toUser.getId())) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        // Check if already friends
        if (friendshipRepository.existsByUserIdAndFriendId(userId, toUser.getId())) {
            throw new BadRequestException("Already friends with this user");
        }

        // Check if pending request already exists
        if (friendRequestRepository.existsPendingRequest(userId, toUser.getId())) {
            throw new BadRequestException("Friend request already sent");
        }

        // Check if the other user has a pending request to us (auto-accept)
        var reverseRequest = friendRequestRepository.findPendingRequest(toUser.getId(), userId);
        if (reverseRequest.isPresent()) {
            // Auto-accept both ways
            return acceptFriendRequest(userId, reverseRequest.get().getId());
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .message(request.getMessage())
                .status(FriendRequest.RequestStatus.pending)
                .build();

        friendRequest = friendRequestRepository.save(friendRequest);
        log.info("Friend request sent from user {} to user {}", userId, toUser.getId());

        return FriendRequestResponse.from(friendRequest);
    }

    /**
     * Get all friend requests for a user
     */
    public List<FriendRequestResponse> getFriendRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findByToUserId(userId);
        return requests.stream()
                .map(FriendRequestResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get pending friend requests
     */
    public List<FriendRequestResponse> getPendingFriendRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findPendingByToUserId(userId);
        return requests.stream()
                .map(FriendRequestResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Accept a friend request
     */
    @Transactional
    public FriendRequestResponse acceptFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (!request.getToUser().getId().equals(userId)) {
            throw new BadRequestException("This friend request is not for you");
        }

        if (request.getStatus() != FriendRequest.RequestStatus.pending) {
            throw new BadRequestException("Friend request already handled");
        }

        // Update request status
        request.setStatus(FriendRequest.RequestStatus.accepted);
        request.setHandledAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        // Create bidirectional friendship
        User user = request.getToUser();
        User friend = request.getFromUser();

        Friendship friendship1 = Friendship.builder()
                .user(user)
                .friend(friend)
                .build();
        friendshipRepository.save(friendship1);

        Friendship friendship2 = Friendship.builder()
                .user(friend)
                .friend(user)
                .build();
        friendshipRepository.save(friendship2);

        log.info("Friend request {} accepted, friendship created between {} and {}",
                requestId, userId, friend.getId());

        return FriendRequestResponse.from(request);
    }

    /**
     * Reject a friend request
     */
    @Transactional
    public FriendRequestResponse rejectFriendRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (!request.getToUser().getId().equals(userId)) {
            throw new BadRequestException("This friend request is not for you");
        }

        if (request.getStatus() != FriendRequest.RequestStatus.pending) {
            throw new BadRequestException("Friend request already handled");
        }

        request.setStatus(FriendRequest.RequestStatus.rejected);
        request.setHandledAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        log.info("Friend request {} rejected by user {}", requestId, userId);

        return FriendRequestResponse.from(request);
    }

    /**
     * Delete a friend (unfriend)
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        // Delete bidirectional friendship
        int deleted1 = friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        int deleted2 = friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);

        if (deleted1 == 0 && deleted2 == 0) {
            throw new NotFoundException("Friendship not found");
        }

        log.info("Friendship deleted between {} and {}", userId, friendId);
    }

    /**
     * Update friend remark
     */
    @Transactional
    public void updateRemark(Long userId, Long friendId, String remark) {
        int updated = friendshipRepository.updateRemark(userId, friendId, remark);
        if (updated == 0) {
            throw new NotFoundException("Friendship not found");
        }
        log.info("Remark updated for friend {} by user {}", friendId, userId);
    }

    /**
     * Block a friend
     */
    @Transactional
    public void blockFriend(Long userId, Long friendId) {
        int updated = friendshipRepository.updateBlockStatus(userId, friendId, true);
        if (updated == 0) {
            throw new NotFoundException("Friendship not found");
        }
        log.info("User {} blocked by user {}", friendId, userId);
    }

    /**
     * Unblock a friend
     */
    @Transactional
    public void unblockFriend(Long userId, Long friendId) {
        int updated = friendshipRepository.updateBlockStatus(userId, friendId, false);
        if (updated == 0) {
            throw new NotFoundException("Friendship not found");
        }
        log.info("User {} unblocked by user {}", friendId, userId);
    }
}
