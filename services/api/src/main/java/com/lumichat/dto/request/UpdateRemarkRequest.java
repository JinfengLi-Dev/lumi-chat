package com.lumichat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRemarkRequest {
    @Size(max = 50, message = "Remark must be at most 50 characters")
    private String remark;
}
