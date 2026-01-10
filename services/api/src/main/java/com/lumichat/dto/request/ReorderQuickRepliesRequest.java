package com.lumichat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderQuickRepliesRequest {

    @NotEmpty(message = "IDs list is required")
    private List<Long> ids;
}
