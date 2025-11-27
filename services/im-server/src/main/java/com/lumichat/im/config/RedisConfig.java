package com.lumichat.im.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.protocol.Packet;
import com.lumichat.im.protocol.ProtocolType;
import com.lumichat.im.service.MessageProcessor;
import com.lumichat.im.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to message channels for fan-out
        container.addMessageListener(messageListener(), new PatternTopic("im:messages"));
        container.addMessageListener(typingListener(), new PatternTopic("im:typing"));
        container.addMessageListener(readStatusListener(), new PatternTopic("im:read_status"));
        container.addMessageListener(recallListener(), new PatternTopic("im:recall"));

        return container;
    }

    @Bean
    public MessageListener messageListener() {
        return (Message message, byte[] pattern) -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);

                Long conversationId = ((Number) data.get("conversationId")).longValue();
                Long senderId = ((Number) data.get("senderId")).longValue();
                String senderDeviceId = (String) data.get("senderDeviceId");

                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = (Map<String, Object>) data.get("message");

                // TODO: Get conversation participants from API/cache
                // For now, broadcast to sender's other devices as a demo
                var senderSessions = sessionManager.getSessionsByUserId(senderId);
                for (var session : senderSessions) {
                    // Don't send back to the originating device
                    if (!session.getDeviceId().equals(senderDeviceId)) {
                        Packet packet = Packet.of(ProtocolType.RECEIVE_MESSAGE, Map.of(
                                "conversationId", conversationId,
                                "senderId", senderId,
                                "message", messageData
                        ));
                        messageProcessor.sendToUserDevice(senderId, session.getDeviceId(), packet);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process Redis message", e);
            }
        };
    }

    @Bean
    public MessageListener typingListener() {
        return (Message message, byte[] pattern) -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);
                Long userId = ((Number) data.get("userId")).longValue();
                Long conversationId = ((Number) data.get("conversationId")).longValue();

                // TODO: Send typing notification to other participants
                log.debug("Typing notification: userId={}, conversationId={}", userId, conversationId);
            } catch (Exception e) {
                log.error("Failed to process typing notification", e);
            }
        };
    }

    @Bean
    public MessageListener readStatusListener() {
        return (Message message, byte[] pattern) -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);
                Long userId = ((Number) data.get("userId")).longValue();
                String deviceId = (String) data.get("deviceId");
                Long conversationId = ((Number) data.get("conversationId")).longValue();
                Long lastReadMsgId = ((Number) data.get("lastReadMsgId")).longValue();

                // Sync read status to user's other devices
                var sessions = sessionManager.getSessionsByUserId(userId);
                for (var session : sessions) {
                    if (!session.getDeviceId().equals(deviceId)) {
                        Packet packet = Packet.of(ProtocolType.READ_ACK, Map.of(
                                "conversationId", conversationId,
                                "lastReadMsgId", lastReadMsgId
                        ));
                        messageProcessor.sendToUserDevice(userId, session.getDeviceId(), packet);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process read status", e);
            }
        };
    }

    @Bean
    public MessageListener recallListener() {
        return (Message message, byte[] pattern) -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);
                Long userId = ((Number) data.get("userId")).longValue();
                String msgId = (String) data.get("msgId");

                // TODO: Broadcast recall to all conversation participants
                log.info("Message recalled: userId={}, msgId={}", userId, msgId);
            } catch (Exception e) {
                log.error("Failed to process recall", e);
            }
        };
    }
}
