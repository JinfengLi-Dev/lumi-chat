package com.lumichat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferOwnershipRequest {
    @NotNull(message = "New owner ID is required")
    private Long newOwnerId;
}
