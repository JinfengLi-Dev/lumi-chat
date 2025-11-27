package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email or UID is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Device type is required")
    private String deviceType;

    private String deviceName;

    private Boolean rememberMe;
}
