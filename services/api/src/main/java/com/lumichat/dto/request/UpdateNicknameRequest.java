package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateNicknameRequest {
    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 30, message = "Nickname must be 2-30 characters")
    private String nickname;
}
