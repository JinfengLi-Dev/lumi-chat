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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

    private void registerDevice(User user, LoginRequest request, String ipAddress) {
        UserDevice.DeviceType deviceType;
        try {
            deviceType = UserDevice.DeviceType.valueOf(request.getDeviceType().toLowerCase());
        } catch (IllegalArgumentException e) {
            deviceType = UserDevice.DeviceType.web;
        }

        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(user.getId(), request.getDeviceId())
                .orElse(UserDevice.builder()
                        .user(user)
                        .deviceId(request.getDeviceId())
                        .deviceType(deviceType)
                        .build());

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
