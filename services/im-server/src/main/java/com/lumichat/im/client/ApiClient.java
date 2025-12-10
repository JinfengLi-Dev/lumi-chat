package com.lumichat.im.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for calling the backend API server.
 * Handles message persistence, conversation queries, and other API calls.
 */
@Slf4j
@Component
public class ApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    public ApiClient(
            ObjectMapper objectMapper,
            @Value("${api.base-url:http://localhost:8080}") String apiBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Persist a message to the database via the API server.
     * Uses internal service-to-service authentication.
     */
    public MessagePersistResult persistMessage(Long senderId, String deviceId, PersistMessageRequest request) {
        try {
            String url = apiBaseUrl + "/internal/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "im-server");
            headers.set("X-User-Id", senderId.toString());
            headers.set("X-Device-Id", deviceId);

            Map<String, Object> body = Map.of(
                    "conversationId", request.conversationId(),
                    "msgType", request.msgType(),
                    "content", request.content() != null ? request.content() : "",
                    "metadata", request.metadata() != null ? request.metadata() : "",
                    "quoteMsgId", request.quoteMsgId() != null ? request.quoteMsgId() : "",
                    "atUserIds", request.atUserIds() != null ? request.atUserIds() : List.of(),
                    "clientMsgId", request.clientMsgId()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                return new MessagePersistResult(
                        true,
                        data.path("msgId").asText(),
                        data.path("serverTimestamp").asText(),
                        null
                );
            } else {
                log.error("Failed to persist message: {}", response.getBody());
                return new MessagePersistResult(false, null, null, "API error");
            }
        } catch (Exception e) {
            log.error("Error persisting message", e);
            return new MessagePersistResult(false, null, null, e.getMessage());
        }
    }

    /**
     * Recall a message via the API server.
     */
    public RecallResult recallMessage(Long userId, String msgId) {
        try {
            String url = apiBaseUrl + "/internal/messages/" + msgId + "/recall";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "im-server");
            headers.set("X-User-Id", userId.toString());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return new RecallResult(true, null);
            } else {
                JsonNode root = objectMapper.readTree(response.getBody());
                String error = root.path("message").asText("Unknown error");
                return new RecallResult(false, error);
            }
        } catch (Exception e) {
            log.error("Error recalling message", e);
            return new RecallResult(false, e.getMessage());
        }
    }

    /**
     * Get conversation participants.
     */
    public List<Long> getConversationParticipants(Long conversationId) {
        try {
            String url = apiBaseUrl + "/internal/conversations/" + conversationId + "/participants";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service", "im-server");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                if (data.isArray()) {
                    return objectMapper.convertValue(data,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
                }
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error getting conversation participants for conversationId={}", conversationId, e);
            return List.of();
        }
    }

    /**
     * Get messages for sync (missed messages).
     */
    public List<Map<String, Object>> getMessagesForSync(Long userId, Long conversationId, Long afterMsgId, int limit) {
        try {
            String url = apiBaseUrl + "/internal/conversations/" + conversationId + "/messages"
                    + "?afterId=" + (afterMsgId != null ? afterMsgId : 0)
                    + "&limit=" + limit;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service", "im-server");
            headers.set("X-User-Id", userId.toString());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                if (data.isArray()) {
                    return objectMapper.convertValue(data,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                }
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error getting messages for sync", e);
            return List.of();
        }
    }

    // Request/Response DTOs
    public record PersistMessageRequest(
            Long conversationId,
            String msgType,
            String content,
            String metadata,
            String quoteMsgId,
            List<Long> atUserIds,
            String clientMsgId
    ) {}

    public record MessagePersistResult(
            boolean success,
            String msgId,
            String serverTimestamp,
            String error
    ) {}

    public record RecallResult(
            boolean success,
            String error
    ) {}

    /**
     * Queue a message for offline user delivery.
     */
    public QueueMessageResult queueOfflineMessage(Long targetUserId, String targetDeviceId,
                                                   Long messageId, Long conversationId) {
        try {
            String url = apiBaseUrl + "/sync/queue";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "im-server");

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("targetUserId", targetUserId);
            body.put("messageId", messageId);
            body.put("conversationId", conversationId);
            if (targetDeviceId != null) {
                body.put("targetDeviceId", targetDeviceId);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return new QueueMessageResult(true, null);
            } else {
                JsonNode root = objectMapper.readTree(response.getBody());
                String error = root.path("message").asText("Unknown error");
                return new QueueMessageResult(false, error);
            }
        } catch (Exception e) {
            log.error("Error queuing offline message for user {}", targetUserId, e);
            return new QueueMessageResult(false, e.getMessage());
        }
    }

    /**
     * Get pending offline messages for a user/device.
     */
    public List<Map<String, Object>> getPendingOfflineMessages(Long userId, String deviceId, int limit) {
        try {
            String url = apiBaseUrl + "/sync/messages"
                    + "?deviceId=" + deviceId
                    + "&limit=" + limit;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service", "im-server");
            headers.set("X-User-Id", userId.toString());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data").path("messages");

                if (data.isArray()) {
                    return objectMapper.convertValue(data,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                }
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error getting pending offline messages for user {}", userId, e);
            return List.of();
        }
    }

    /**
     * Acknowledge offline message delivery.
     */
    public boolean acknowledgeOfflineMessages(Long userId, String deviceId, List<Long> messageIds) {
        try {
            String url = apiBaseUrl + "/sync/ack";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "im-server");
            headers.set("X-User-Id", userId.toString());

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("deviceId", deviceId);
            body.put("messageIds", messageIds);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error acknowledging offline messages for user {}", userId, e);
            return false;
        }
    }

    public record QueueMessageResult(
            boolean success,
            String error
    ) {}

    /**
     * Update read status for a conversation.
     * Returns info about who to notify (for private chats, the message sender).
     */
    public ReadStatusResult updateReadStatus(Long userId, Long conversationId, Long lastReadMsgId) {
        try {
            String url = apiBaseUrl + "/internal/conversations/" + conversationId + "/read";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "im-server");
            headers.set("X-User-Id", userId.toString());

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("lastReadMsgId", lastReadMsgId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                Long notifyUserId = data.has("notifyUserId") && !data.path("notifyUserId").isNull()
                        ? data.path("notifyUserId").asLong()
                        : null;

                return new ReadStatusResult(
                        true,
                        conversationId,
                        lastReadMsgId,
                        userId,
                        notifyUserId,
                        null
                );
            } else {
                log.error("Failed to update read status: {}", response.getBody());
                return new ReadStatusResult(false, conversationId, lastReadMsgId, userId, null, "API error");
            }
        } catch (Exception e) {
            log.error("Error updating read status for user {} conversation {}", userId, conversationId, e);
            return new ReadStatusResult(false, conversationId, lastReadMsgId, userId, null, e.getMessage());
        }
    }

    public record ReadStatusResult(
            boolean success,
            Long conversationId,
            Long lastReadMsgId,
            Long readerId,
            Long notifyUserId,  // The user to notify (in private chats, the message sender)
            String error
    ) {}
}
