package com.lumichat.repository;

import com.lumichat.entity.UserConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConversationRepository extends JpaRepository<UserConversation, Long> {

    Optional<UserConversation> findByUserIdAndConversationId(Long userId, Long conversationId);

    @Query("SELECT uc FROM UserConversation uc " +
           "JOIN FETCH uc.conversation c " +
           "WHERE uc.user.id = :userId AND uc.isHidden = false " +
           "ORDER BY uc.isPinned DESC, c.lastMsgTime DESC NULLS LAST")
    List<UserConversation> findAllByUserIdOrderByPinnedAndTime(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(uc.unreadCount), 0) FROM UserConversation uc " +
           "WHERE uc.user.id = :userId AND uc.isHidden = false AND uc.isMuted = false")
    Integer getTotalUnreadCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.unreadCount = 0, uc.lastReadMsgId = :lastMsgId " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void markAsRead(@Param("userId") Long userId, @Param("conversationId") Long conversationId, @Param("lastMsgId") Long lastMsgId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.isMuted = :muted " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void setMuted(@Param("userId") Long userId, @Param("conversationId") Long conversationId, @Param("muted") boolean muted);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.isPinned = :pinned " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void setPinned(@Param("userId") Long userId, @Param("conversationId") Long conversationId, @Param("pinned") boolean pinned);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.isHidden = true " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void softDelete(@Param("userId") Long userId, @Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.draft = :draft " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void saveDraft(@Param("userId") Long userId, @Param("conversationId") Long conversationId, @Param("draft") String draft);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.unreadCount = uc.unreadCount + 1 " +
           "WHERE uc.conversation.id = :conversationId AND uc.user.id != :senderId")
    void incrementUnreadForOthers(@Param("conversationId") Long conversationId, @Param("senderId") Long senderId);

    @Modifying
    @Query("UPDATE UserConversation uc SET uc.clearedAt = CURRENT_TIMESTAMP, uc.unreadCount = 0 " +
           "WHERE uc.user.id = :userId AND uc.conversation.id = :conversationId")
    void clearMessages(@Param("userId") Long userId, @Param("conversationId") Long conversationId);
}
