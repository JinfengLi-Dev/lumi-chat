package com.lumichat.dto.response;

import com.lumichat.entity.Message;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {

    private Long id;
    private String msgId;
    private Long conversationId;
    private Long senderId;
    private UserResponse sender;
    private String senderDeviceId;
    private String msgType;
    private String content;
    private String metadata;
    private String quoteMsgId;
    private MessageResponse quoteMessage;
    private String atUserIds;
    private LocalDateTime clientCreatedAt;
    private LocalDateTime serverCreatedAt;
    private LocalDateTime recalledAt;
    private String status;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .msgId(message.getMsgId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderDeviceId(message.getSenderDeviceId())
                .msgType(message.getMsgType().name())
                .content(message.getContent())
                .metadata(message.getMetadata())
                .quoteMsgId(message.getQuoteMsgId())
                .atUserIds(message.getAtUserIds())
                .clientCreatedAt(message.getClientCreatedAt())
                .serverCreatedAt(message.getServerCreatedAt())
                .recalledAt(message.getRecalledAt())
                .status("sent")
                .build();
    }

    public static MessageResponse fromWithSender(Message message) {
        MessageResponse response = from(message);
        if (message.getSender() != null) {
            response.setSender(UserResponse.from(message.getSender()));
        }
        return response;
    }
}
