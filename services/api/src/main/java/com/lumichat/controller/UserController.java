package com.lumichat.controller;

import com.lumichat.dto.request.ChangePasswordRequest;
import com.lumichat.dto.request.UpdateProfileRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.FileResponse;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.FileStorageService;
import com.lumichat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    /**
     * Get current user profile
     * GET /users/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.getCurrentUser(principal.getId());
        return ApiResponse.success(user);
    }

    /**
     * Update current user profile
     * PUT /users/me
     */
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = userService.updateProfile(principal.getId(), request);
        return ApiResponse.success(user);
    }

    /**
     * Upload user avatar
     * POST /users/me/avatar
     */
    @PostMapping("/me/avatar")
    public ApiResponse<AvatarResponse> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ApiResponse.error("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error("File must be an image");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error("File size must be less than 5MB");
        }

        // Upload to MinIO
        FileResponse fileResponse = fileStorageService.uploadAvatar(principal.getId(), file);
        String avatarUrl = fileResponse.getUrl();

        UserResponse user = userService.updateAvatar(principal.getId(), avatarUrl);
        return ApiResponse.success(new AvatarResponse(user.getAvatar()));
    }

    /**
     * Change user password
     * PUT /users/me/password
     */
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ApiResponse.success();
    }

    /**
     * Search users by email or UID
     * GET /users/search?q=xxx
     */
    @GetMapping("/search")
    public ApiResponse<List<UserResponse>> searchUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("q") String query) {
        List<UserResponse> users = userService.searchUsers(query, principal.getId());
        return ApiResponse.success(users);
    }

    /**
     * Get user by UID
     * GET /users/{uid}
     */
    @GetMapping("/{uid}")
    public ApiResponse<UserResponse> getUserByUid(@PathVariable String uid) {
        UserResponse user = userService.getUserResponseByUid(uid);
        return ApiResponse.success(user);
    }

    /**
     * Upload voice introduction
     * POST /users/me/voice-intro
     */
    @PostMapping("/me/voice-intro")
    public ApiResponse<UserResponse> uploadVoiceIntro(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "duration", required = false) Integer duration) {

        if (file.isEmpty()) {
            return ApiResponse.error("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            return ApiResponse.error("File must be an audio file");
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ApiResponse.error("File size must be less than 10MB");
        }

        // Validate duration (max 60 seconds for voice intro)
        if (duration != null && duration > 60) {
            return ApiResponse.error("Voice introduction must be 60 seconds or less");
        }

        // Upload to MinIO
        FileResponse fileResponse = fileStorageService.uploadVoice(principal.getId(), file);
        String voiceIntroUrl = fileResponse.getUrl();

        UserResponse user = userService.updateVoiceIntro(principal.getId(), voiceIntroUrl, duration);
        return ApiResponse.success(user);
    }

    /**
     * Delete voice introduction
     * DELETE /users/me/voice-intro
     */
    @DeleteMapping("/me/voice-intro")
    public ApiResponse<UserResponse> deleteVoiceIntro(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = userService.deleteVoiceIntro(principal.getId());
        return ApiResponse.success(user);
    }

    // Inner class for avatar response
    public record AvatarResponse(String url) {}
}
