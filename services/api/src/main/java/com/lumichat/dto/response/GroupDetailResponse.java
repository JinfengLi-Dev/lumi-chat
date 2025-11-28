package com.lumichat.dto.response;

import com.lumichat.entity.Group;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroupDetailResponse {
    private Long id;
    private String gid;
    private String name;
    private String avatar;
    private Long ownerId;
    private String ownerNickname;
    private String announcement;
    private Integer maxMembers;
    private Integer memberCount;
    private LocalDateTime createdAt;

    public static GroupDetailResponse from(Group group, int memberCount) {
        return GroupDetailResponse.builder()
                .id(group.getId())
                .gid(group.getGid())
                .name(group.getName())
                .avatar(group.getAvatar())
                .ownerId(group.getOwner() != null ? group.getOwner().getId() : null)
                .ownerNickname(group.getOwner() != null ? group.getOwner().getNickname() : null)
                .announcement(group.getAnnouncement())
                .maxMembers(group.getMaxMembers())
                .memberCount(memberCount)
                .createdAt(group.getCreatedAt())
                .build();
    }
}
