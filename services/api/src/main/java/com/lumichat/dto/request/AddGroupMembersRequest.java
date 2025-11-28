package com.lumichat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddGroupMembersRequest {
    @NotEmpty(message = "Member IDs are required")
    private List<Long> memberIds;
}
