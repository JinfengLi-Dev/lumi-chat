package com.lumichat.controller;

import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.RegisterRequest;
import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.LoginResponse;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = getClientIp(servletRequest);
        LoginResponse response = authService.login(request, ipAddress);
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = getClientIp(servletRequest);
        LoginResponse response = authService.refreshToken(request.refreshToken(), ipAddress);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId(), principal.getDeviceId());
        return ApiResponse.success();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        // Always returns success to prevent email enumeration attacks
        authService.initiatePasswordReset(request.email());
        return ApiResponse.success();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    // Inner classes for simple request bodies
    public record RefreshTokenRequest(String refreshToken) {}
    public record ForgotPasswordRequest(String email) {}
    public record ResetPasswordRequest(String token, String newPassword) {}
}
