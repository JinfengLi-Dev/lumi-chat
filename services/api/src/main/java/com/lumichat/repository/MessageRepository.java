package com.lumichat.repository;

import com.lumichat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByMsgId(String msgId);

    Page<Message> findByConversationIdOrderByServerCreatedAtDesc(
            Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.id < :beforeId ORDER BY m.serverCreatedAt DESC")
    Page<Message> findBeforeId(@Param("conversationId") Long conversationId,
                               @Param("beforeId") Long beforeId,
                               Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.serverCreatedAt > :since ORDER BY m.serverCreatedAt ASC")
    List<Message> findNewMessages(@Param("conversationId") Long conversationId,
                                  @Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.msgType = 'text' " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND m.recalledAt IS NULL " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> searchMessages(@Param("conversationId") Long conversationId,
                                 @Param("query") String query,
                                 Pageable pageable);

    // Queries that filter by clearedAt timestamp (for clear chat history feature)
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND (:clearedAt IS NULL OR m.serverCreatedAt > :clearedAt) " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> findByConversationIdAfterClearedAt(
            @Param("conversationId") Long conversationId,
            @Param("clearedAt") LocalDateTime clearedAt,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.id < :beforeId " +
           "AND (:clearedAt IS NULL OR m.serverCreatedAt > :clearedAt) " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> findBeforeIdAfterClearedAt(
            @Param("conversationId") Long conversationId,
            @Param("beforeId") Long beforeId,
            @Param("clearedAt") LocalDateTime clearedAt,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.msgType = 'text' " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND m.recalledAt IS NULL " +
           "AND (:clearedAt IS NULL OR m.serverCreatedAt > :clearedAt) " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> searchMessagesAfterClearedAt(
            @Param("conversationId") Long conversationId,
            @Param("query") String query,
            @Param("clearedAt") LocalDateTime clearedAt,
            Pageable pageable);
}
