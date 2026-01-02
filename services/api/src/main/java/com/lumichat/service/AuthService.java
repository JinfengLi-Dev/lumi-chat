package com.lumichat.service;

import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.RegisterRequest;
import com.lumichat.dto.response.LoginResponse;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.entity.User;
import com.lumichat.entity.UserDevice;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.UnauthorizedException;
import com.lumichat.repository.UserDeviceRepository;
import com.lumichat.repository.UserRepository;
import com.lumichat.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        // Find user by email or UID
        User user = userRepository.findByEmailOrUid(request.getEmail(), request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email/UID or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email/UID or password");
        }

        // Check if user is active
        if (user.getStatus() != User.UserStatus.active) {
            throw new UnauthorizedException("Account is " + user.getStatus().name());
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Register or update device
        registerDevice(user, request, ipAddress);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), request.getDeviceId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), request.getDeviceId());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.from(user))
                .expiresAt(System.currentTimeMillis() + jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // Generate unique UID
        String uid = generateUniqueUid();

        // Create user
        User user = User.builder()
                .uid(uid)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .gender(User.Gender.valueOf(request.getGender()))
                .status(User.UserStatus.active)
                .build();

        user = userRepository.save(user);

        // Send welcome email asynchronously
        emailService.sendWelcomeEmail(request.getEmail(), request.getNickname());

        return UserResponse.from(user);
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken, String ipAddress) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceIdFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getStatus() != User.UserStatus.active) {
            throw new UnauthorizedException("Account is " + user.getStatus().name());
        }

        // Update device last active
        userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(device -> {
                    device.setLastActiveAt(LocalDateTime.now());
                    userDeviceRepository.save(device);
                });

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, deviceId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, deviceId);

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(UserResponse.from(user))
                .expiresAt(System.currentTimeMillis() + jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Transactional
    public void logout(Long userId, String deviceId) {
        userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(device -> {
                    device.setIsOnline(false);
                    device.setPushToken(null);
                    userDeviceRepository.save(device);
                });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validate the reset token (it should be a JWT token with password-reset type)
        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }

        // Verify it's actually a password-reset token, not an access/refresh token
        if (!jwtTokenProvider.isPasswordResetToken(token)) {
            throw new UnauthorizedException("Invalid token type");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successful for user: {}", userId);
    }

    public void initiatePasswordReset(String email) {
        // Find user by email - always return success to prevent email enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            // Generate password reset token
            String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getId());

            // Build reset URL
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            // Send email asynchronously
            emailService.sendPasswordResetEmail(email, resetUrl);

            log.info("Password reset email initiated for user: {}", user.getId());
        });
    }

    private void registerDevice(User user, LoginRequest request, String ipAddress) {
        UserDevice.DeviceType deviceType;
        try {
            deviceType = UserDevice.DeviceType.valueOf(request.getDeviceType().toLowerCase());
        } catch (IllegalArgumentException e) {
            deviceType = UserDevice.DeviceType.web;
        }

        // First check if device exists globally (could be registered to another user)
        UserDevice device = userDeviceRepository.findByDeviceId(request.getDeviceId())
                .orElse(null);

        if (device != null) {
            // Device exists - reassign to current user if different
            if (!device.getUser().getId().equals(user.getId())) {
                device.setUser(user);
            }
            device.setDeviceType(deviceType);
        } else {
            // Create new device
            device = UserDevice.builder()
                    .user(user)
                    .deviceId(request.getDeviceId())
                    .deviceType(deviceType)
                    .build();
        }

        device.setDeviceName(request.getDeviceName());
        device.setIsOnline(true);
        device.setLastActiveAt(LocalDateTime.now());

        userDeviceRepository.save(device);
    }

    private String generateUniqueUid() {
        String uid;
        do {
            uid = "LC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByUid(uid));
        return uid;
    }
}
