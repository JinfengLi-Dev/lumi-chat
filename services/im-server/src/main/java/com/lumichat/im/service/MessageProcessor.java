package com.lumichat.im.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.protocol.*;
import com.lumichat.im.session.SessionManager;
import com.lumichat.im.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public void handleLogin(ChannelHandlerContext ctx, Packet packet) {
        try {
            LoginData loginData = objectMapper.convertValue(packet.getData(), LoginData.class);

            // TODO: Validate token with API server
            // For now, extract userId from a mock token format "userId:deviceId"
            Long userId = extractUserIdFromToken(loginData.getToken());

            if (userId != null) {
                sessionManager.addSession(ctx.channel(), userId,
                        loginData.getDeviceId(), loginData.getDeviceType());

                // Publish online status to Redis for other services
                redisTemplate.opsForSet().add("online:users", userId.toString());

                sendResponse(ctx, ProtocolType.LOGIN_RESPONSE, packet.getSeq(),
                        Map.of("success", true, "userId", userId));

                log.info("User logged in: userId={}, deviceId={}", userId, loginData.getDeviceId());
            } else {
                sendResponse(ctx, ProtocolType.LOGIN_RESPONSE, packet.getSeq(),
                        Map.of("success", false, "error", "Invalid token"));
            }
        } catch (Exception e) {
            log.error("Login failed", e);
            sendResponse(ctx, ProtocolType.LOGIN_RESPONSE, packet.getSeq(),
                    Map.of("success", false, "error", e.getMessage()));
        }
    }

    public void handleLogout(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session != null) {
            handleDisconnect(session);
            sendResponse(ctx, ProtocolType.LOGOUT_RESPONSE, packet.getSeq(),
                    Map.of("success", true));
        }
        ctx.close();
    }

    public void handleHeartbeat(ChannelHandlerContext ctx, Packet packet) {
        sendResponse(ctx, ProtocolType.HEARTBEAT_RESPONSE, packet.getSeq(),
                Map.of("serverTime", System.currentTimeMillis()));
    }

    public void handleChatMessage(ChannelHandlerContext ctx, Packet packet) {
        UserSession senderSession = sessionManager.getSessionByChannel(ctx.channel());
        if (senderSession == null) {
            log.warn("Received message from unauthenticated channel");
            return;
        }

        try {
            ChatMessageData msgData = objectMapper.convertValue(packet.getData(), ChatMessageData.class);

            // TODO: Persist message to database via API call

            // Send ACK back to sender
            sendResponse(ctx, ProtocolType.CHAT_MESSAGE_ACK, packet.getSeq(),
                    Map.of("msgId", msgData.getMsgId(),
                            "serverTimestamp", System.currentTimeMillis(),
                            "success", true));

            // Publish message to Redis for fan-out to all participants
            String messageJson = objectMapper.writeValueAsString(Map.of(
                    "type", "chat_message",
                    "senderId", senderSession.getUserId(),
                    "senderDeviceId", senderSession.getDeviceId(),
                    "conversationId", msgData.getConversationId(),
                    "message", msgData
            ));
            redisTemplate.convertAndSend("im:messages", messageJson);

            log.debug("Message processed: msgId={}, from={}", msgData.getMsgId(), senderSession.getUserId());
        } catch (Exception e) {
            log.error("Failed to process chat message", e);
            sendResponse(ctx, ProtocolType.CHAT_MESSAGE_ACK, packet.getSeq(),
                    Map.of("success", false, "error", e.getMessage()));
        }
    }

    public void handleTyping(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long conversationId = ((Number) data.get("conversationId")).longValue();

            // Publish typing notification to Redis
            String typingJson = objectMapper.writeValueAsString(Map.of(
                    "type", "typing",
                    "userId", session.getUserId(),
                    "conversationId", conversationId
            ));
            redisTemplate.convertAndSend("im:typing", typingJson);
        } catch (Exception e) {
            log.error("Failed to process typing notification", e);
        }
    }

    public void handleReadAck(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long conversationId = ((Number) data.get("conversationId")).longValue();
            Long lastReadMsgId = ((Number) data.get("lastReadMsgId")).longValue();

            // Publish read status to Redis for sync to other devices
            String readJson = objectMapper.writeValueAsString(Map.of(
                    "type", "read_status",
                    "userId", session.getUserId(),
                    "deviceId", session.getDeviceId(),
                    "conversationId", conversationId,
                    "lastReadMsgId", lastReadMsgId
            ));
            redisTemplate.convertAndSend("im:read_status", readJson);
        } catch (Exception e) {
            log.error("Failed to process read ack", e);
        }
    }

    public void handleRecall(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            String msgId = (String) data.get("msgId");

            // TODO: Validate ownership and time window via API

            // Publish recall notification to Redis
            String recallJson = objectMapper.writeValueAsString(Map.of(
                    "type", "recall",
                    "userId", session.getUserId(),
                    "msgId", msgId
            ));
            redisTemplate.convertAndSend("im:recall", recallJson);
        } catch (Exception e) {
            log.error("Failed to process recall", e);
        }
    }

    public void handleSyncRequest(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        // TODO: Fetch missed messages from API and send to client
        sendResponse(ctx, ProtocolType.SYNC_RESPONSE, packet.getSeq(),
                Map.of("messages", java.util.Collections.emptyList(),
                        "syncCursor", System.currentTimeMillis()));
    }

    public void handleDisconnect(UserSession session) {
        // Remove from Redis online set if no other devices
        var remainingSessions = sessionManager.getSessionsByUserId(session.getUserId());
        if (remainingSessions.size() <= 1) {
            redisTemplate.opsForSet().remove("online:users", session.getUserId().toString());
        }

        log.info("User disconnected: userId={}, deviceId={}",
                session.getUserId(), session.getDeviceId());
    }

    public void sendToUser(Long userId, Packet packet) {
        var sessions = sessionManager.getSessionsByUserId(userId);
        for (UserSession session : sessions) {
            sendPacket(session, packet);
        }
    }

    public void sendToUserDevice(Long userId, String deviceId, Packet packet) {
        UserSession session = sessionManager.getSession(userId, deviceId);
        if (session != null) {
            sendPacket(session, packet);
        }
    }

    private void sendPacket(UserSession session, Packet packet) {
        try {
            String json = objectMapper.writeValueAsString(packet);
            session.getChannel().writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("Failed to send packet to user {}", session.getUserId(), e);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, int type, String seq, Object data) {
        try {
            Packet response = Packet.response(type, seq, data);
            String json = objectMapper.writeValueAsString(response);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("Failed to send response", e);
        }
    }

    private Long extractUserIdFromToken(String token) {
        // TODO: Implement proper JWT validation
        // For development, accept format "userId" directly
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
