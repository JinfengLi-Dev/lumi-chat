package com.lumichat.dto.response;

import com.lumichat.entity.FriendRequest;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendRequestResponse {
    private Long id;
    private UserResponse fromUser;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    public static FriendRequestResponse from(FriendRequest request) {
        return FriendRequestResponse.builder()
                .id(request.getId())
                .fromUser(UserResponse.from(request.getFromUser()))
                .message(request.getMessage())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .handledAt(request.getHandledAt())
                .build();
    }
}
