package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[\\p{So}\\p{Sk}]+$", message = "Must be a valid emoji")
    private String emoji;
}
