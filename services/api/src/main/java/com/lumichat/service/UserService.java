package com.lumichat.service;

import com.lumichat.dto.request.ChangePasswordRequest;
import com.lumichat.dto.request.UpdateProfileRequest;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.entity.User;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user by ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Get user by UID
     */
    public User getUserByUid(String uid) {
        return userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Get current user profile
     */
    public UserResponse getCurrentUser(Long userId) {
        User user = getUserById(userId);
        return UserResponse.from(user);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname());
        }

        if (request.getGender() != null) {
            try {
                user.setGender(User.Gender.valueOf(request.getGender().toLowerCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender value: " + request.getGender() +
                    ". Valid values are: male, female, unknown");
            }
        }

        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        log.info("Updated profile for user: {}", userId);

        return UserResponse.from(user);
    }

    /**
     * Update user avatar URL
     */
    @Transactional
    public UserResponse updateAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        user.setAvatar(avatarUrl);
        user = userRepository.save(user);
        log.info("Updated avatar for user: {}", userId);
        return UserResponse.from(user);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Changed password for user: {}", userId);
    }

    /**
     * Search users by query (email or UID)
     */
    public List<UserResponse> searchUsers(String query, Long currentUserId) {
        if (query == null || query.length() < 2) {
            throw new BadRequestException("Search query must be at least 2 characters");
        }

        // Search by email or UID containing the query
        List<User> users = userRepository.findByEmailContainingIgnoreCaseOrUidContainingIgnoreCase(
                query, query);

        return users.stream()
                .filter(u -> !u.getId().equals(currentUserId)) // Exclude self
                .filter(u -> u.getStatus() == User.UserStatus.active) // Only active users
                .limit(20) // Limit results
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Get user response by UID (for viewing other user's profile)
     */
    public UserResponse getUserResponseByUid(String uid) {
        User user = getUserByUid(uid);
        if (user.getStatus() != User.UserStatus.active) {
            throw new NotFoundException("User not found");
        }
        return UserResponse.from(user);
    }

    /**
     * Update user's voice introduction
     */
    @Transactional
    public UserResponse updateVoiceIntro(Long userId, String voiceIntroUrl, Integer duration) {
        User user = getUserById(userId);
        user.setVoiceIntroUrl(voiceIntroUrl);
        user.setVoiceIntroDuration(duration);
        user = userRepository.save(user);
        log.info("Updated voice introduction for user: {}", userId);
        return UserResponse.from(user);
    }

    /**
     * Delete user's voice introduction
     */
    @Transactional
    public UserResponse deleteVoiceIntro(Long userId) {
        User user = getUserById(userId);
        user.setVoiceIntroUrl(null);
        user.setVoiceIntroDuration(null);
        user = userRepository.save(user);
        log.info("Deleted voice introduction for user: {}", userId);
        return UserResponse.from(user);
    }

    /**
     * Check if UID is available (not taken by another user)
     */
    public boolean isUidAvailable(Long currentUserId, String uid) {
        User currentUser = getUserById(currentUserId);

        // If the UID is the same as current user's UID, it's available
        if (currentUser.getUid().equals(uid)) {
            return true;
        }

        // Check if UID is taken by another user
        return !userRepository.existsByUid(uid);
    }

    /**
     * Update user's UID
     */
    @Transactional
    public UserResponse updateUid(Long userId, String newUid) {
        User user = getUserById(userId);

        // Check if UID is same as current
        if (user.getUid().equals(newUid)) {
            return UserResponse.from(user);
        }

        // Check if UID is already taken
        if (userRepository.existsByUid(newUid)) {
            throw new BadRequestException("UID is already in use");
        }

        user.setUid(newUid);
        user = userRepository.save(user);
        log.info("Updated UID for user {} to {}", userId, newUid);
        return UserResponse.from(user);
    }
}
