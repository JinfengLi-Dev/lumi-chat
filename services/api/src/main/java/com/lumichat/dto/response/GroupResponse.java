package com.lumichat.dto.response;

import com.lumichat.entity.Group;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroupResponse {

    private Long id;
    private String gid;
    private String name;
    private String avatar;
    private Long ownerId;
    private UserResponse owner;
    private Long creatorId;
    private UserResponse creator;
    private String announcement;
    private Integer maxMembers;
    private Integer memberCount;
    private LocalDateTime createdAt;

    public static GroupResponse from(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .gid(group.getGid())
                .name(group.getName())
                .avatar(group.getAvatar())
                .ownerId(group.getOwner() != null ? group.getOwner().getId() : null)
                .creatorId(group.getCreator() != null ? group.getCreator().getId() : null)
                .announcement(group.getAnnouncement())
                .maxMembers(group.getMaxMembers())
                .memberCount(group.getMemberCount())
                .createdAt(group.getCreatedAt())
                .build();
    }

    public static GroupResponse fromWithOwner(Group group) {
        GroupResponse response = from(group);
        if (group.getOwner() != null) {
            response.setOwner(UserResponse.from(group.getOwner()));
        }
        if (group.getCreator() != null) {
            response.setCreator(UserResponse.from(group.getCreator()));
        }
        return response;
    }
}
