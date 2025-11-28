package com.lumichat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    private String nickname;

    private String gender;

    @Size(max = 200, message = "Signature must be at most 200 characters")
    private String signature;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;
}
