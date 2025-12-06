package com.lumichat.im.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.client.ApiClient;
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

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;
    private final ObjectMapper objectMapper;
    private final ApiClient apiClient;

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
                String msgId = (String) data.get("msgId");

                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = (Map<String, Object>) data.get("message");

                // Get conversation participants from API
                List<Long> participants = apiClient.getConversationParticipants(conversationId);

                if (participants.isEmpty()) {
                    log.warn("No participants found for conversation {}", conversationId);
                    return;
                }

                // Broadcast message to all participants
                for (Long participantId : participants) {
                    var sessions = sessionManager.getSessionsByUserId(participantId);
                    for (var session : sessions) {
                        // Don't send back to the originating device
                        if (participantId.equals(senderId) && session.getDeviceId().equals(senderDeviceId)) {
                            continue;
                        }

                        Packet packet = Packet.of(ProtocolType.RECEIVE_MESSAGE, Map.of(
                                "conversationId", conversationId,
                                "senderId", senderId,
                                "msgId", msgId,
                                "message", messageData
                        ));
                        messageProcessor.sendToUserDevice(participantId, session.getDeviceId(), packet);
                    }
                }

                log.debug("Message broadcast to {} participants for conversation {}",
                        participants.size(), conversationId);
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

                // Get conversation participants and send typing notification
                List<Long> participants = apiClient.getConversationParticipants(conversationId);

                for (Long participantId : participants) {
                    // Don't notify the user who is typing
                    if (participantId.equals(userId)) {
                        continue;
                    }

                    Packet packet = Packet.of(ProtocolType.TYPING_NOTIFY, Map.of(
                            "conversationId", conversationId,
                            "userId", userId
                    ));
                    messageProcessor.sendToUser(participantId, packet);
                }

                log.debug("Typing notification sent: userId={}, conversationId={}", userId, conversationId);
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
                Long conversationId = data.get("conversationId") != null
                        ? ((Number) data.get("conversationId")).longValue()
                        : null;

                if (conversationId == null || conversationId == 0) {
                    log.warn("Recall notification missing conversationId for msgId={}", msgId);
                    return;
                }

                // Get conversation participants and broadcast recall notification
                List<Long> participants = apiClient.getConversationParticipants(conversationId);

                for (Long participantId : participants) {
                    Packet packet = Packet.of(ProtocolType.RECALL_NOTIFY, Map.of(
                            "conversationId", conversationId,
                            "msgId", msgId,
                            "recalledBy", userId
                    ));
                    messageProcessor.sendToUser(participantId, packet);
                }

                log.info("Recall notification broadcast: userId={}, msgId={}, participants={}",
                        userId, msgId, participants.size());
            } catch (Exception e) {
                log.error("Failed to process recall", e);
            }
        };
    }
}
