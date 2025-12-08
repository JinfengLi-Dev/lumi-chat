package com.lumichat.dto.response;

import com.lumichat.entity.Friendship;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendResponse {
    private Long id;
    private String uid;
    private String nickname;
    private String avatar;
    private String remark;
    private String signature;
    private String gender;
    private String status;
    private LocalDateTime friendshipCreatedAt;

    public static FriendResponse from(Friendship friendship) {
        var friend = friendship.getFriend();
        return FriendResponse.builder()
                .id(friend.getId())
                .uid(friend.getUid())
                .nickname(friend.getNickname())
                .avatar(friend.getAvatar())
                .remark(friendship.getRemark())
                .signature(friend.getSignature())
                .gender(friend.getGender() != null ? friend.getGender().name() : null)
                .status(friendship.getStatus())
                .friendshipCreatedAt(friendship.getCreatedAt())
                .build();
    }
}
