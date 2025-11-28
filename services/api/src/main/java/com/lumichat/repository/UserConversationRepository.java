package com.lumichat.repository;

import com.lumichat.entity.UserConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConversationRepository extends JpaRepository<UserConversation, Long> {

    Optional<UserConversation> findByUserIdAndConversationId(Long userId, Long conversationId);

    @Query("SELECT uc FROM UserConversation uc " +
           "JOIN FETCH uc.conversation c " +
           "WHERE uc.user.id = :userId AND uc.isDeleted = false " +
           "ORDER BY uc.isPinned DESC, c.lastMsgTime DESC NULLS LAST")
    List<UserConversation> findAllByUserIdOrderByPinnedAndTime(Long userId);

    @Query("SELECT COALESCE(SUM(uc.unreadCount), 0) FROM UserConversation uc " +
           "WHERE uc.user.id = :userId AND uc.isDeleted = false AND uc.isMuted = false")
    Integer getTotalUnreadCount(Long userId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.unreadCount = 0, uc.lastReadMsgId = :lastMsgId " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void markAsRead(Long userId, Long conversationId, Long lastMsgId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.isMuted = :muted " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void setMuted(Long userId, Long conversationId, boolean muted);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.isPinned = :pinned " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void setPinned(Long userId, Long conversationId, boolean pinned);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.isDeleted = true " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void softDelete(Long userId, Long conversationId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.draft = :draft " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void saveDraft(Long userId, Long conversationId, String draft);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.unreadCount = uc.unreadCount + 1 " +
           "WHERE uc.conversation.id = :conversationId AND uc.user.id != :senderId")
    void incrementUnreadForOthers(Long conversationId, Long senderId);
}
