package com.lumichat.im.session;

import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserSession {

    private Long userId;
    private String deviceId;
    private String deviceType;
    private Channel channel;
    private Instant connectedAt;
    private Instant lastActiveAt;

    public void updateLastActive() {
        this.lastActiveAt = Instant.now();
    }

    public boolean isActive(long timeoutMs) {
        return lastActiveAt != null &&
                Instant.now().toEpochMilli() - lastActiveAt.toEpochMilli() < timeoutMs;
    }
}
