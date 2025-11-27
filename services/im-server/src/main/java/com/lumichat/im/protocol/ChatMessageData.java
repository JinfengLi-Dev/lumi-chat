package com.lumichat.im.protocol;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatMessageData {
    private String msgId;          // Client-generated unique message ID
    private Long conversationId;
    private String msgType;        // text, image, file, voice, video, location, user_card, group_card
    private String content;
    private Map<String, Object> metadata;
    private String quoteMsgId;
    private List<Long> atUserIds;
    private Long clientTimestamp;
}
