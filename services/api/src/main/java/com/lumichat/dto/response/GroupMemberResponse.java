package com.lumichat.dto.response;

import com.lumichat.entity.GroupMember;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroupMemberResponse {
    private Long id;
    private Long userId;
    private String uid;
    private String nickname;
    private String groupNickname;
    private String avatar;
    private String role;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse from(GroupMember member) {
        var user = member.getUser();
        return GroupMemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .uid(user.getUid())
                .nickname(user.getNickname())
                .groupNickname(member.getNickname())
                .avatar(user.getAvatar())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
