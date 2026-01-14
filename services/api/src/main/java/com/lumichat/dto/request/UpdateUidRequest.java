package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUidRequest {
    @NotBlank(message = "UID is required")
    @Size(min = 3, max = 20, message = "UID must be 3-20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "UID must contain only letters, numbers, and underscores")
    private String uid;
}
