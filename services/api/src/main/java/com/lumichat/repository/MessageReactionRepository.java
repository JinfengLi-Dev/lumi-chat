package com.lumichat.repository;

import com.lumichat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    /**
     * Find all reactions for a specific message
     */
    List<MessageReaction> findByMessageId(Long messageId);

    /**
     * Find a specific reaction by message, user, and emoji
     */
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    /**
     * Check if a user has reacted with a specific emoji to a message
     */
    boolean existsByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    /**
     * Delete a specific reaction
     */
    @Modifying
    @Query("DELETE FROM MessageReaction mr WHERE mr.message.id = :messageId AND mr.user.id = :userId AND mr.emoji = :emoji")
    void deleteByMessageIdAndUserIdAndEmoji(@Param("messageId") Long messageId,
                                            @Param("userId") Long userId,
                                            @Param("emoji") String emoji);

    /**
     * Get aggregated reaction counts for a message
     */
    @Query("SELECT mr.emoji, COUNT(mr.id) FROM MessageReaction mr WHERE mr.message.id = :messageId GROUP BY mr.emoji")
    List<Object[]> getReactionCountsByMessageId(@Param("messageId") Long messageId);

    /**
     * Get all users who reacted with a specific emoji
     */
    @Query("SELECT mr.user.id FROM MessageReaction mr WHERE mr.message.id = :messageId AND mr.emoji = :emoji")
    List<Long> getUserIdsByMessageIdAndEmoji(@Param("messageId") Long messageId, @Param("emoji") String emoji);

    /**
     * Delete all reactions for a message (used when message is deleted)
     */
    @Modifying
    @Query("DELETE FROM MessageReaction mr WHERE mr.message.id = :messageId")
    void deleteAllByMessageId(@Param("messageId") Long messageId);
}
