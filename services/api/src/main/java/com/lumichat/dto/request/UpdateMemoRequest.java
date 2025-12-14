package com.lumichat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMemoRequest {
    @Size(max = 2000, message = "Memo must be at most 2000 characters")
    private String memo;
}
