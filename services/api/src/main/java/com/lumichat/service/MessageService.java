package com.lumichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.SendMessageRequest;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.entity.Conversation;
import com.lumichat.entity.Message;
import com.lumichat.entity.User;
import com.lumichat.entity.UserConversation;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.repository.MessageRepository;
import com.lumichat.repository.UserConversationRepository;
import com.lumichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserConversationRepository userConversationRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_CHANNEL_MESSAGES = "im:messages";

    /**
     * Get messages for a conversation with pagination
     * Filters by clearedAt timestamp to support clear chat history feature
     */
    public List<MessageResponse> getMessages(Long userId, Long conversationId, Long beforeId, int limit) {
        // Get user's conversation settings including clearedAt timestamp
        UserConversation uc = userConversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        LocalDateTime clearedAt = uc.getClearedAt();

        Page<Message> messages;
        if (beforeId != null) {
            messages = messageRepository.findBeforeIdAfterClearedAt(
                    conversationId, beforeId, clearedAt, PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findByConversationIdAfterClearedAt(
                    conversationId, clearedAt, PageRequest.of(0, limit));
        }

        return messages.getContent().stream()
                .map(MessageResponse::fromWithSender)
                .toList();
    }

    /**
     * Send a new message
     */
    @Transactional
    public MessageResponse sendMessage(Long userId, String deviceId, SendMessageRequest request) {
        // Verify user has access to conversation
        userConversationRepository.findByUserIdAndConversationId(userId, request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Parse message type
        Message.MessageType msgType;
        try {
            msgType = Message.MessageType.valueOf(request.getMsgType().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid message type: " + request.getMsgType());
        }

        // Parse client created time
        LocalDateTime clientCreatedAt = null;
        if (request.getClientCreatedAt() != null && !request.getClientCreatedAt().isBlank()) {
            try {
                clientCreatedAt = LocalDateTime.parse(request.getClientCreatedAt(),
                        DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                log.warn("Could not parse client created time: {}", request.getClientCreatedAt());
            }
        }

        // Create message
        Message message = Message.builder()
                .msgId(generateMsgId())
                .conversation(conversation)
                .sender(sender)
                .senderDeviceId(deviceId)
                .msgType(msgType)
                .content(request.getContent())
                .metadata(request.getMetadata())
                .quoteMsgId(request.getQuoteMsgId())
                .atUserIds(request.getAtUserIds())
                .clientCreatedAt(clientCreatedAt)
                .build();

        message = messageRepository.save(message);

        // Update conversation last message
        conversation.setLastMsgTime(message.getServerCreatedAt());
        conversation.setLastMsgId(message.getId());
        conversationRepository.save(conversation);

        // Increment unread count for other participants
        userConversationRepository.incrementUnreadForOthers(conversation.getId(), userId);

        // Publish message to Redis for real-time delivery
        publishMessageToRedis(userId, deviceId, message, sender);

        log.info("User {} sent message {} to conversation {}",
                userId, message.getMsgId(), conversation.getId());

        return MessageResponse.fromWithSender(message);
    }

    /**
     * Recall a message
     */
    @Transactional
    public void recallMessage(Long userId, String msgId) {
        Message message = messageRepository.findByMsgId(msgId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify sender
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("Cannot recall message sent by another user");
        }

        // Check time limit (2 minutes)
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        if (message.getServerCreatedAt().isBefore(twoMinutesAgo)) {
            throw new RuntimeException("Cannot recall message after 2 minutes");
        }

        // Mark as recalled
        message.setRecalledAt(LocalDateTime.now());
        message.setMsgType(Message.MessageType.recall);
        messageRepository.save(message);

        log.info("User {} recalled message {}", userId, msgId);
    }

    /**
     * Forward a message to another conversation
     */
    @Transactional
    public MessageResponse forwardMessage(Long userId, String deviceId, String msgId, Long targetConversationId) {
        Message originalMessage = messageRepository.findByMsgId(msgId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify user has access to target conversation
        userConversationRepository.findByUserIdAndConversationId(userId, targetConversationId)
                .orElseThrow(() -> new RuntimeException("Target conversation not found"));

        Conversation targetConversation = conversationRepository.findById(targetConversationId)
                .orElseThrow(() -> new RuntimeException("Target conversation not found"));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create forwarded message
        Message message = Message.builder()
                .msgId(generateMsgId())
                .conversation(targetConversation)
                .sender(sender)
                .senderDeviceId(deviceId)
                .msgType(originalMessage.getMsgType())
                .content(originalMessage.getContent())
                .metadata(originalMessage.getMetadata())
                .build();

        message = messageRepository.save(message);

        // Update conversation last message
        targetConversation.setLastMsgTime(message.getServerCreatedAt());
        targetConversation.setLastMsgId(message.getId());
        conversationRepository.save(targetConversation);

        // Increment unread count for other participants
        userConversationRepository.incrementUnreadForOthers(targetConversation.getId(), userId);

        // Publish forwarded message to Redis for real-time delivery
        publishMessageToRedis(userId, deviceId, message, sender);

        log.info("User {} forwarded message {} to conversation {}",
                userId, msgId, targetConversationId);

        return MessageResponse.fromWithSender(message);
    }

    /**
     * Search messages in a conversation
     * Filters by clearedAt timestamp to support clear chat history feature
     */
    public List<MessageResponse> searchMessages(Long userId, Long conversationId, String query, int page, int limit) {
        // Get user's conversation settings including clearedAt timestamp
        UserConversation uc = userConversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        Page<Message> messages = messageRepository.searchMessagesAfterClearedAt(
                conversationId, query.trim(), uc.getClearedAt(), PageRequest.of(page, limit));

        return messages.getContent().stream()
                .map(MessageResponse::fromWithSender)
                .toList();
    }

    /**
     * Delete a message (soft delete for user)
     */
    @Transactional
    public void deleteMessage(Long userId, String msgId) {
        Message message = messageRepository.findByMsgId(msgId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify user has access to the conversation
        userConversationRepository.findByUserIdAndConversationId(userId, message.getConversation().getId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Only sender can delete their own messages
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        messageRepository.delete(message);
        log.info("User {} deleted message {}", userId, msgId);
    }

    private String generateMsgId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String truncateContent(String content, Message.MessageType msgType) {
        if (content == null) {
            return "[" + msgType.name() + "]";
        }
        if (msgType != Message.MessageType.text) {
            return "[" + msgType.name() + "]";
        }
        if (content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content;
    }

    /**
     * Publish message to Redis for real-time delivery to online users.
     * If publishing fails, the message is still saved to DB and will be delivered via offline queue.
     */
    private void publishMessageToRedis(Long userId, String deviceId, Message message, User sender) {
        try {
            // Build sender info for display
            Map<String, Object> senderInfo = new LinkedHashMap<>();
            senderInfo.put("id", sender.getId());
            senderInfo.put("nickname", sender.getNickname());
            senderInfo.put("avatar", sender.getAvatar() != null ? sender.getAvatar() : "");

            // Build message payload matching frontend expectations
            Map<String, Object> messagePayload = new LinkedHashMap<>();
            messagePayload.put("id", message.getId());
            messagePayload.put("msgId", message.getMsgId());
            messagePayload.put("conversationId", message.getConversation().getId());
            messagePayload.put("msgType", message.getMsgType().toString());
            messagePayload.put("content", message.getContent() != null ? message.getContent() : "");
            // Parse metadata JSON string to Object to avoid double-encoding
            // Without this, the JSON string gets escaped again when serialized
            Object metadataObj = null;
            if (message.getMetadata() != null) {
                try {
                    metadataObj = objectMapper.readValue(message.getMetadata(), Object.class);
                } catch (Exception e) {
                    log.warn("Failed to parse metadata for message {}: {}", message.getMsgId(), e.getMessage());
                    metadataObj = message.getMetadata(); // fallback to string
                }
            }
            messagePayload.put("metadata", metadataObj);
            messagePayload.put("quoteMsgId", message.getQuoteMsgId());
            messagePayload.put("atUserIds", message.getAtUserIds());
            messagePayload.put("senderId", userId);
            messagePayload.put("senderDeviceId", deviceId);
            messagePayload.put("serverCreatedAt", message.getServerCreatedAt().toString());
            messagePayload.put("sender", senderInfo);

            // Build Redis event matching RedisConfig.messageListener() expectations
            // IMPORTANT: msgId must be the database ID (Long as String) for offline queue compatibility
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "chat_message");
            event.put("senderId", userId);
            event.put("senderDeviceId", deviceId);
            event.put("conversationId", message.getConversation().getId());
            event.put("msgId", String.valueOf(message.getId())); // Use DB ID for offline queue
            event.put("message", messagePayload);

            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(REDIS_CHANNEL_MESSAGES, json);

            log.debug("Published message {} to Redis channel {}", message.getMsgId(), REDIS_CHANNEL_MESSAGES);
        } catch (Exception e) {
            // Don't throw - message is saved to DB, will be delivered via offline queue when user reconnects
            log.error("Failed to publish message {} to Redis (will be delivered via offline queue): {}",
                    message.getMsgId(), e.getMessage());
        }
    }
}
