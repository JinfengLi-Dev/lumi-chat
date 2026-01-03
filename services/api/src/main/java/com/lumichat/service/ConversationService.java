package com.lumichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.response.ConversationResponse;
import com.lumichat.dto.response.MessageResponse;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.entity.Conversation;
import com.lumichat.entity.User;
import com.lumichat.entity.UserConversation;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.ConversationRepository;
import com.lumichat.repository.MessageRepository;
import com.lumichat.repository.UserConversationRepository;
import com.lumichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserConversationRepository userConversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all conversations for a user.
     * Optimized to batch-load users and messages to avoid N+1 queries.
     */
    public List<ConversationResponse> getUserConversations(Long userId) {
        List<UserConversation> userConversations = userConversationRepository
                .findAllByUserIdOrderByPinnedAndTime(userId);

        if (userConversations.isEmpty()) {
            return List.of();
        }

        // Collect all conversation IDs and target user IDs for batch loading
        List<Long> conversationIds = new ArrayList<>();
        Set<Long> targetUserIds = new java.util.HashSet<>();

        for (UserConversation uc : userConversations) {
            Conversation c = uc.getConversation();
            conversationIds.add(c.getId());

            // For private chats, collect the other participant's ID
            if (c.getType() == Conversation.ConversationType.private_chat ||
                c.getType() == Conversation.ConversationType.stranger) {
                List<Long> participantIds = arrayToList(c.getParticipantIds());
                participantIds.stream()
                        .filter(id -> !id.equals(userId))
                        .findFirst()
                        .ifPresent(targetUserIds::add);
            }
        }

        // Batch load all target users in one query
        Map<Long, UserResponse> userMap;
        try {
            userMap = userRepository.findAllById(targetUserIds).stream()
                    .collect(Collectors.toMap(User::getId, UserResponse::from));
        } catch (Exception e) {
            log.error("Error batch loading users: {}", e.getMessage());
            userMap = Map.of();
        }

        // Batch load latest messages for all conversations in one query
        Map<Long, MessageResponse> lastMessageMap;
        try {
            lastMessageMap = messageRepository
                    .findLatestMessagesForConversations(conversationIds).stream()
                    .collect(Collectors.toMap(
                            m -> m.getConversation().getId(),
                            MessageResponse::fromWithSender,
                            (m1, m2) -> m1  // In case of duplicates, keep the first
                    ));
        } catch (Exception e) {
            log.error("Error batch loading messages: {}", e.getMessage());
            lastMessageMap = Map.of();
        }

        // Build responses using pre-loaded data
        List<ConversationResponse> responses = new ArrayList<>();
        for (UserConversation uc : userConversations) {
            try {
                ConversationResponse response = buildConversationResponseOptimized(
                        uc, userId, userMap, lastMessageMap);
                responses.add(response);
            } catch (Exception e) {
                log.error("Error building conversation response for conversation {}: {}",
                        uc.getConversation().getId(), e.getMessage());
            }
        }
        return responses;
    }

    /**
     * Get a single conversation by ID
     */
    public ConversationResponse getConversation(Long userId, Long conversationId) {
        UserConversation uc = userConversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (uc.getIsHidden()) {
            throw new NotFoundException("Conversation not found");
        }

        return buildConversationResponse(uc, userId);
    }

    /**
     * Delete a conversation (soft delete for user)
     */
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        userConversationRepository.softDelete(userId, conversationId);
        log.info("User {} deleted conversation {}", userId, conversationId);
    }

    /**
     * Mark conversation as read
     */
    @Transactional
    public void markAsRead(Long userId, Long conversationId) {
        // Get the latest message ID
        var messages = messageRepository.findByConversationIdOrderByServerCreatedAtDesc(
                conversationId, PageRequest.of(0, 1));

        Long lastMsgId = messages.hasContent() ? messages.getContent().get(0).getId() : null;
        userConversationRepository.markAsRead(userId, conversationId, lastMsgId);
        log.info("User {} marked conversation {} as read", userId, conversationId);
    }

    /**
     * Toggle mute status
     */
    @Transactional
    public void toggleMute(Long userId, Long conversationId, boolean muted) {
        userConversationRepository.setMuted(userId, conversationId, muted);
        log.info("User {} set conversation {} muted={}", userId, conversationId, muted);
    }

    /**
     * Toggle pin status
     */
    @Transactional
    public void togglePin(Long userId, Long conversationId, boolean pinned) {
        userConversationRepository.setPinned(userId, conversationId, pinned);
        log.info("User {} set conversation {} pinned={}", userId, conversationId, pinned);
    }

    /**
     * Save draft
     */
    @Transactional
    public void saveDraft(Long userId, Long conversationId, String draft) {
        userConversationRepository.saveDraft(userId, conversationId, draft);
    }

    /**
     * Clear messages in a conversation for a user
     */
    @Transactional
    public void clearMessages(Long userId, Long conversationId) {
        userConversationRepository.clearMessages(userId, conversationId);
        log.info("User {} cleared messages in conversation {}", userId, conversationId);
    }

    /**
     * Update chat background for a conversation
     */
    @Transactional
    public void updateBackground(Long userId, Long conversationId, String backgroundUrl) {
        UserConversation uc = userConversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        uc.setBackgroundUrl(backgroundUrl);
        userConversationRepository.save(uc);
        log.info("User {} updated background for conversation {}", userId, conversationId);
    }

    /**
     * Create or get private conversation between two users
     */
    @Transactional
    public ConversationResponse getOrCreatePrivateConversation(Long userId, Long targetUserId) {
        // Check if conversation already exists
        List<UserConversation> existingUc = userConversationRepository
                .findAllByUserIdOrderByPinnedAndTime(userId);

        for (UserConversation uc : existingUc) {
            if (uc.getConversation().getType() == Conversation.ConversationType.private_chat) {
                List<Long> participantIds = arrayToList(uc.getConversation().getParticipantIds());
                if (participantIds.contains(targetUserId) && participantIds.contains(userId)) {
                    // Found existing conversation
                    if (uc.getIsHidden()) {
                        uc.setIsHidden(false);
                        userConversationRepository.save(uc);
                    }
                    return buildConversationResponse(uc, userId);
                }
            }
        }

        // Create new conversation
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Target user not found"));

        Long[] participantIdsArray = new Long[]{userId, targetUserId};

        Conversation conversation = Conversation.builder()
                .type(Conversation.ConversationType.private_chat)
                .participantIds(participantIdsArray)
                .build();
        conversation = conversationRepository.save(conversation);

        // Create user conversation entries for both users
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Current user not found"));

        UserConversation uc1 = UserConversation.builder()
                .user(currentUser)
                .conversation(conversation)
                .build();
        userConversationRepository.save(uc1);

        UserConversation uc2 = UserConversation.builder()
                .user(targetUser)
                .conversation(conversation)
                .build();
        userConversationRepository.save(uc2);

        log.info("Created private conversation {} between users {} and {}",
                conversation.getId(), userId, targetUserId);

        return buildConversationResponse(uc1, userId);
    }

    /**
     * Create or get stranger conversation between two users (for non-friends)
     */
    @Transactional
    public ConversationResponse getOrCreateStrangerConversation(Long userId, Long targetUserId) {
        // Check if conversation already exists (either stranger or private_chat)
        List<UserConversation> existingUc = userConversationRepository
                .findAllByUserIdOrderByPinnedAndTime(userId);

        for (UserConversation uc : existingUc) {
            Conversation.ConversationType type = uc.getConversation().getType();
            if (type == Conversation.ConversationType.stranger ||
                type == Conversation.ConversationType.private_chat) {
                List<Long> participantIds = arrayToList(uc.getConversation().getParticipantIds());
                if (participantIds.contains(targetUserId) && participantIds.contains(userId)) {
                    // Found existing conversation
                    if (uc.getIsHidden()) {
                        uc.setIsHidden(false);
                        userConversationRepository.save(uc);
                    }
                    return buildConversationResponse(uc, userId);
                }
            }
        }

        // Create new conversation with stranger type
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Target user not found"));

        Long[] participantIdsArray = new Long[]{userId, targetUserId};

        Conversation conversation = Conversation.builder()
                .type(Conversation.ConversationType.stranger)
                .participantIds(participantIdsArray)
                .build();
        conversation = conversationRepository.save(conversation);

        // Create user conversation entries for both users
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Current user not found"));

        UserConversation uc1 = UserConversation.builder()
                .user(currentUser)
                .conversation(conversation)
                .build();
        userConversationRepository.save(uc1);

        UserConversation uc2 = UserConversation.builder()
                .user(targetUser)
                .conversation(conversation)
                .build();
        userConversationRepository.save(uc2);

        log.info("Created stranger conversation {} between users {} and {}",
                conversation.getId(), userId, targetUserId);

        return buildConversationResponse(uc1, userId);
    }

    /**
     * Build a conversation response using pre-loaded data.
     * Used by getUserConversations() for optimized batch loading.
     */
    private ConversationResponse buildConversationResponseOptimized(
            UserConversation uc,
            Long currentUserId,
            Map<Long, UserResponse> userMap,
            Map<Long, MessageResponse> lastMessageMap) {

        Conversation c = uc.getConversation();
        UserResponse targetUser = null;
        MessageResponse lastMessage = null;

        // Get target user from pre-loaded map for private chats
        if (c.getType() == Conversation.ConversationType.private_chat ||
            c.getType() == Conversation.ConversationType.stranger) {
            List<Long> participantIds = arrayToList(c.getParticipantIds());
            Long targetUserId = participantIds.stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (targetUserId != null) {
                targetUser = userMap.get(targetUserId);
            }
        }

        // Get last message from pre-loaded map
        // Note: We don't filter by clearedAt here as the batch query doesn't support it
        // For the conversation list, showing the latest message is acceptable
        lastMessage = lastMessageMap.get(c.getId());

        // If the message is before clearedAt, don't show it
        if (lastMessage != null && uc.getClearedAt() != null &&
            lastMessage.getServerCreatedAt() != null &&
            lastMessage.getServerCreatedAt().isBefore(uc.getClearedAt())) {
            lastMessage = null;
        }

        return ConversationResponse.from(uc, targetUser, lastMessage);
    }

    /**
     * Build a conversation response with all details.
     * Used for single conversation lookups where batch loading isn't needed.
     */
    private ConversationResponse buildConversationResponse(UserConversation uc, Long currentUserId) {
        Conversation c = uc.getConversation();
        UserResponse targetUser = null;
        MessageResponse lastMessage = null;

        // Get target user for private chats
        if (c.getType() == Conversation.ConversationType.private_chat ||
            c.getType() == Conversation.ConversationType.stranger) {
            List<Long> participantIds = arrayToList(c.getParticipantIds());
            Long targetUserId = participantIds.stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (targetUserId != null) {
                targetUser = userRepository.findById(targetUserId)
                        .map(UserResponse::from)
                        .orElse(null);
            }
        }

        // Get last message (filtered by clearedAt so cleared chats show empty)
        var messages = messageRepository.findByConversationIdAfterClearedAt(
                c.getId(), uc.getClearedAt(), PageRequest.of(0, 1));
        if (messages.hasContent()) {
            lastMessage = MessageResponse.fromWithSender(messages.getContent().get(0));
        }

        return ConversationResponse.from(uc, targetUser, lastMessage);
    }

    private List<Long> arrayToList(Long[] array) {
        if (array == null) {
            return List.of();
        }
        return java.util.Arrays.asList(array);
    }
}
