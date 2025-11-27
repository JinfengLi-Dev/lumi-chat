package com.lumichat.security;

import com.lumichat.entity.User;
import lombok.Getter;

@Getter
public class UserPrincipal {

    private final Long id;
    private final String uid;
    private final String email;
    private final String nickname;
    private final String deviceId;

    public UserPrincipal(User user, String deviceId) {
        this.id = user.getId();
        this.uid = user.getUid();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.deviceId = deviceId;
    }
}
