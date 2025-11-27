package com.lumichat.im.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SessionManager {

    // Channel ID -> UserSession
    private final Map<String, UserSession> channelSessions = new ConcurrentHashMap<>();

    // User ID -> DeviceId -> UserSession (for multi-device support)
    private final Map<Long, Map<String, UserSession>> userSessions = new ConcurrentHashMap<>();

    public void addSession(Channel channel, Long userId, String deviceId, String deviceType) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .channel(channel)
                .connectedAt(Instant.now())
                .lastActiveAt(Instant.now())
                .build();

        channelSessions.put(channel.id().asLongText(), session);
        userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(deviceId, session);

        log.info("Session added: userId={}, deviceId={}, channelId={}",
                userId, deviceId, channel.id().asShortText());
    }

    public void removeSession(Channel channel) {
        UserSession session = channelSessions.remove(channel.id().asLongText());
        if (session != null) {
            Map<String, UserSession> devices = userSessions.get(session.getUserId());
            if (devices != null) {
                devices.remove(session.getDeviceId());
                if (devices.isEmpty()) {
                    userSessions.remove(session.getUserId());
                }
            }
            log.info("Session removed: userId={}, deviceId={}",
                    session.getUserId(), session.getDeviceId());
        }
    }

    public UserSession getSessionByChannel(Channel channel) {
        return channelSessions.get(channel.id().asLongText());
    }

    public Collection<UserSession> getSessionsByUserId(Long userId) {
        Map<String, UserSession> devices = userSessions.get(userId);
        return devices != null ? devices.values() : java.util.Collections.emptyList();
    }

    public UserSession getSession(Long userId, String deviceId) {
        Map<String, UserSession> devices = userSessions.get(userId);
        return devices != null ? devices.get(deviceId) : null;
    }

    public void updateLastActive(Channel channel) {
        UserSession session = channelSessions.get(channel.id().asLongText());
        if (session != null) {
            session.updateLastActive();
        }
    }

    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }

    public int getOnlineUserCount() {
        return userSessions.size();
    }

    public Collection<UserSession> getAllSessions() {
        return channelSessions.values();
    }

    public void cleanInactiveSessions(long timeoutMs) {
        var inactiveSessions = channelSessions.values().stream()
                .filter(s -> !s.isActive(timeoutMs))
                .collect(Collectors.toList());

        for (UserSession session : inactiveSessions) {
            log.warn("Cleaning inactive session: userId={}, deviceId={}",
                    session.getUserId(), session.getDeviceId());
            session.getChannel().close();
        }
    }
}
