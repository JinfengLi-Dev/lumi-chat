package com.lumichat.dto.response;

import com.lumichat.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String uid;
    private String email;
    private String nickname;
    private String avatar;
    private String gender;
    private String signature;
    private String description;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .uid(user.getUid())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender().name())
                .signature(user.getSignature())
                .description(user.getDescription())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
