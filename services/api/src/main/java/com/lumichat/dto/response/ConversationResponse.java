package com.lumichat.dto.response;

import com.lumichat.entity.Conversation;
import com.lumichat.entity.UserConversation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {

    private Long id;
    private String type;
    private Long groupId;
    private GroupResponse group;
    private UserResponse targetUser;
    private MessageResponse lastMessage;
    private LocalDateTime lastMsgTime;
    private Integer unreadCount;
    private Boolean isMuted;
    private Boolean isPinned;
    private String draft;
    private Long[] atMsgIds;

    public static ConversationResponse from(UserConversation uc, UserResponse targetUser, MessageResponse lastMessage) {
        Conversation c = uc.getConversation();
        return ConversationResponse.builder()
                .id(c.getId())
                .type(c.getType().name())
                .groupId(c.getGroup() != null ? c.getGroup().getId() : null)
                .targetUser(targetUser)
                .lastMessage(lastMessage)
                .lastMsgTime(c.getLastMsgTime())
                .unreadCount(uc.getUnreadCount())
                .isMuted(uc.getIsMuted())
                .isPinned(uc.getIsPinned())
                .draft(uc.getDraft())
                .atMsgIds(uc.getAtMsgIds())
                .build();
    }
}
