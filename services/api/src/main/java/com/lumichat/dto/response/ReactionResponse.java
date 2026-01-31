package com.lumichat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponse {

    private String emoji;
    private Long count;
    private List<Long> userIds;
    private Boolean currentUserReacted;

    public static ReactionResponse of(String emoji, Long count, List<Long> userIds, Long currentUserId) {
        return ReactionResponse.builder()
                .emoji(emoji)
                .count(count)
                .userIds(userIds)
                .currentUserReacted(userIds.contains(currentUserId))
                .build();
    }
}
