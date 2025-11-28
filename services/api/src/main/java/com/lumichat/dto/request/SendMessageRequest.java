package com.lumichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Conversation ID is required")
    private Long conversationId;

    @NotBlank(message = "Message type is required")
    private String msgType;

    private String content;

    private String metadata;

    private String quoteMsgId;

    private Long[] atUserIds;

    private String clientCreatedAt;
}
