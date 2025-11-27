package com.lumichat.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String refreshToken;
    private UserResponse user;
    private Long expiresAt;
}
