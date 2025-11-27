package com.lumichat.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 20, message = "Nickname must be 2-20 characters")
    private String nickname;

    @Pattern(regexp = "male|female", message = "Gender must be male or female")
    private String gender = "unknown";
}
