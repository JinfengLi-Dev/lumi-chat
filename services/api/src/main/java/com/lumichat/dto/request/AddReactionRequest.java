package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddReactionRequest {

    @NotBlank(message = "Emoji is required")
    @Size(max = 10, message = "Emoji must be at most 10 characters")
    private String emoji;
}
