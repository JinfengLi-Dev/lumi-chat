package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendFriendRequestRequest {
    @NotBlank(message = "UID is required")
    private String uid;

    @Size(max = 200, message = "Message must be at most 200 characters")
    private String message;
}
