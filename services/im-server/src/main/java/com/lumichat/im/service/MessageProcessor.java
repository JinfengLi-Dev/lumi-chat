package com.lumichat.im.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.client.ApiClient;
import com.lumichat.im.protocol.*;
import com.lumichat.im.security.JwtTokenValidator;
import com.lumichat.im.session.SessionManager;
import com.lumichat.im.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenValidator jwtTokenValidator;
    private final ApiClient apiClient;

    public void handleLogin(ChannelHandlerContext ctx, Packet packet) {
        try {
            LoginData loginData = objectMapper.convertValue(packet.getData(), LoginData.class);

            // Validate JWT token
            JwtTokenValidator.TokenInfo tokenInfo = jwtTokenValidator.validateToken(loginData.getToken());

            if (tokenInfo != null) {
                Long userId = tokenInfo.userId();
                String deviceId = loginData.getDeviceId();

                // Verify deviceId matches token if provided in token
                if (tokenInfo.deviceId() != null && !tokenInfo.deviceId().equals(deviceId)) {
                    log.warn("Device ID mismatch: token={}, request={}", tokenInfo.deviceId(), deviceId);
                    sendResponse(ctx, ProtocolType.LOGIN_RESPONSE, packet.getSeq(),
                            Map.of("success", false, "error", "Device ID mismatch"));
                    return;
                }

                sessionManager.addSession(ctx.channel(), userId, deviceId, loginData.getDeviceType());

                // Publish online status to Redis for other services
                redisTemplate.opsForSet().add("online:users", userId.toString());

                sendResponse(ctx, ProtocolType.LOGIN_RESPONSE, packet.getSeq(),
                        Map.of("success", true, "userId", userId));

                log.info("User logged in: userId={}, deviceId={}", userId, deviceId);
            } else {
                sendResponse(ctx, ProtocolType.LOGIN_RESPONSE, packet.getSeq(),
                        Map.of("success", false, "error", "Invalid or expired token"));
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

            // Persist message to database via API call
            ApiClient.PersistMessageRequest persistRequest = new ApiClient.PersistMessageRequest(
                    msgData.getConversationId(),
                    msgData.getMsgType(),
                    msgData.getContent(),
                    msgData.getMetadata() != null ? objectMapper.writeValueAsString(msgData.getMetadata()) : null,
                    msgData.getQuoteMsgId(),
                    msgData.getAtUserIds(),
                    msgData.getMsgId()
            );

            ApiClient.MessagePersistResult persistResult = apiClient.persistMessage(
                    senderSession.getUserId(),
                    senderSession.getDeviceId(),
                    persistRequest
            );

            if (!persistResult.success()) {
                log.error("Failed to persist message: {}", persistResult.error());
                sendResponse(ctx, ProtocolType.CHAT_MESSAGE_ACK, packet.getSeq(),
                        Map.of("success", false, "error", persistResult.error()));
                return;
            }

            // Send ACK back to sender with server-assigned msgId
            sendResponse(ctx, ProtocolType.CHAT_MESSAGE_ACK, packet.getSeq(),
                    Map.of("clientMsgId", msgData.getMsgId(),
                            "msgId", persistResult.msgId(),
                            "serverTimestamp", persistResult.serverTimestamp(),
                            "success", true));

            // Publish message to Redis for fan-out to all participants
            String messageJson = objectMapper.writeValueAsString(Map.of(
                    "type", "chat_message",
                    "senderId", senderSession.getUserId(),
                    "senderDeviceId", senderSession.getDeviceId(),
                    "conversationId", msgData.getConversationId(),
                    "msgId", persistResult.msgId(),
                    "message", msgData
            ));
            redisTemplate.convertAndSend("im:messages", messageJson);

            log.debug("Message processed: clientMsgId={}, serverMsgId={}, from={}",
                    msgData.getMsgId(), persistResult.msgId(), senderSession.getUserId());
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
            Long conversationId = data.get("conversationId") != null
                    ? ((Number) data.get("conversationId")).longValue()
                    : null;

            // Validate ownership and time window via API, then persist recall
            ApiClient.RecallResult recallResult = apiClient.recallMessage(session.getUserId(), msgId);

            if (!recallResult.success()) {
                log.warn("Recall failed for msgId={}: {}", msgId, recallResult.error());
                sendResponse(ctx, ProtocolType.RECALL_ACK, packet.getSeq(),
                        Map.of("success", false, "error", recallResult.error()));
                return;
            }

            // Send ACK back to sender
            sendResponse(ctx, ProtocolType.RECALL_ACK, packet.getSeq(),
                    Map.of("success", true, "msgId", msgId));

            // Publish recall notification to Redis for broadcast
            String recallJson = objectMapper.writeValueAsString(Map.of(
                    "type", "recall",
                    "userId", session.getUserId(),
                    "msgId", msgId,
                    "conversationId", conversationId != null ? conversationId : 0
            ));
            redisTemplate.convertAndSend("im:recall", recallJson);

            log.info("Message recalled: userId={}, msgId={}", session.getUserId(), msgId);
        } catch (Exception e) {
            log.error("Failed to process recall", e);
            sendResponse(ctx, ProtocolType.RECALL_ACK, packet.getSeq(),
                    Map.of("success", false, "error", e.getMessage()));
        }
    }

    public void handleSyncRequest(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long conversationId = data.get("conversationId") != null
                    ? ((Number) data.get("conversationId")).longValue()
                    : null;
            Long afterMsgId = data.get("afterMsgId") != null
                    ? ((Number) data.get("afterMsgId")).longValue()
                    : null;
            int limit = data.get("limit") != null
                    ? ((Number) data.get("limit")).intValue()
                    : 50;

            if (conversationId == null) {
                sendResponse(ctx, ProtocolType.SYNC_RESPONSE, packet.getSeq(),
                        Map.of("success", false, "error", "conversationId is required"));
                return;
            }

            // Fetch messages from API
            List<Map<String, Object>> messages = apiClient.getMessagesForSync(
                    session.getUserId(), conversationId, afterMsgId, limit);

            sendResponse(ctx, ProtocolType.SYNC_RESPONSE, packet.getSeq(),
                    Map.of("success", true,
                            "messages", messages,
                            "conversationId", conversationId,
                            "syncCursor", System.currentTimeMillis()));

            log.debug("Sync response sent: userId={}, conversationId={}, messageCount={}",
                    session.getUserId(), conversationId, messages.size());
        } catch (Exception e) {
            log.error("Failed to process sync request", e);
            sendResponse(ctx, ProtocolType.SYNC_RESPONSE, packet.getSeq(),
                    Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Handle offline sync request - fetch and deliver pending offline messages.
     */
    public void handleOfflineSyncRequest(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            int limit = data != null && data.get("limit") != null
                    ? ((Number) data.get("limit")).intValue()
                    : 100;

            // Fetch pending offline messages from API
            List<Map<String, Object>> pendingMessages = apiClient.getPendingOfflineMessages(
                    session.getUserId(), session.getDeviceId(), limit);

            if (pendingMessages.isEmpty()) {
                // No pending messages - send completion notification
                sendResponse(ctx, ProtocolType.OFFLINE_SYNC_COMPLETE, packet.getSeq(),
                        Map.of("success", true, "count", 0));
                log.debug("No offline messages for user {} device {}",
                        session.getUserId(), session.getDeviceId());
                return;
            }

            // Send offline messages to client
            sendResponse(ctx, ProtocolType.OFFLINE_SYNC_RESPONSE, packet.getSeq(),
                    Map.of("success", true,
                            "messages", pendingMessages,
                            "count", pendingMessages.size()));

            log.info("Delivered {} offline messages to user {} device {}",
                    pendingMessages.size(), session.getUserId(), session.getDeviceId());
        } catch (Exception e) {
            log.error("Failed to process offline sync request", e);
            sendResponse(ctx, ProtocolType.OFFLINE_SYNC_RESPONSE, packet.getSeq(),
                    Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Handle offline sync acknowledgment - mark messages as delivered.
     */
    public void handleOfflineSyncAck(ChannelHandlerContext ctx, Packet packet) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();

            @SuppressWarnings("unchecked")
            List<Number> messageIdNumbers = (List<Number>) data.get("messageIds");
            List<Long> messageIds = messageIdNumbers != null
                    ? messageIdNumbers.stream().map(Number::longValue).toList()
                    : List.of();

            if (messageIds.isEmpty()) {
                log.warn("Empty messageIds in offline sync ack from user {}", session.getUserId());
                return;
            }

            // Acknowledge delivery via API
            boolean success = apiClient.acknowledgeOfflineMessages(
                    session.getUserId(), session.getDeviceId(), messageIds);

            if (success) {
                log.debug("Acknowledged {} offline messages for user {} device {}",
                        messageIds.size(), session.getUserId(), session.getDeviceId());
            } else {
                log.warn("Failed to acknowledge offline messages for user {}", session.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to process offline sync ack", e);
        }
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

}
