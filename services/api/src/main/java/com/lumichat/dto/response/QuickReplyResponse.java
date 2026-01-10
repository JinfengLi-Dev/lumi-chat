package com.lumichat.dto.response;

import com.lumichat.entity.QuickReply;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuickReplyResponse {

    private Long id;
    private String content;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public static QuickReplyResponse from(QuickReply quickReply) {
        return QuickReplyResponse.builder()
                .id(quickReply.getId())
                .content(quickReply.getContent())
                .sortOrder(quickReply.getSortOrder())
                .createdAt(quickReply.getCreatedAt())
                .build();
    }
}
