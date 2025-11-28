package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatar;

    @Size(max = 500, message = "Announcement must be at most 500 characters")
    private String announcement;

    private List<Long> memberIds;
}
