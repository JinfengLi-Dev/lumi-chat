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
    // Using COALESCE to handle NULL clearedAt (use a very old date as default)
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.serverCreatedAt > COALESCE(:clearedAt, CAST('1970-01-01' AS timestamp)) " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> findByConversationIdAfterClearedAt(
            @Param("conversationId") Long conversationId,
            @Param("clearedAt") LocalDateTime clearedAt,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.id < :beforeId " +
           "AND m.serverCreatedAt > COALESCE(:clearedAt, CAST('1970-01-01' AS timestamp)) " +
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
           "AND m.serverCreatedAt > COALESCE(:clearedAt, CAST('1970-01-01' AS timestamp)) " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> searchMessagesAfterClearedAt(
            @Param("conversationId") Long conversationId,
            @Param("query") String query,
            @Param("clearedAt") LocalDateTime clearedAt,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.msgType IN :msgTypes " +
           "AND m.recalledAt IS NULL " +
           "AND m.serverCreatedAt > COALESCE(:clearedAt, CAST('1970-01-01' AS timestamp)) " +
           "ORDER BY m.serverCreatedAt DESC")
    Page<Message> findByConversationIdAndMsgTypesAfterClearedAt(
            @Param("conversationId") Long conversationId,
            @Param("msgTypes") List<String> msgTypes,
            @Param("clearedAt") LocalDateTime clearedAt,
            Pageable pageable);

    /**
     * Batch fetch latest message for each conversation.
     * Uses a subquery to get the max serverCreatedAt for each conversation,
     * then fetches those messages.
     */
    @Query("SELECT m FROM Message m WHERE m.id IN (" +
           "SELECT m2.id FROM Message m2 WHERE m2.conversation.id IN :conversationIds " +
           "AND m2.serverCreatedAt = (" +
           "  SELECT MAX(m3.serverCreatedAt) FROM Message m3 " +
           "  WHERE m3.conversation.id = m2.conversation.id" +
           "))")
    List<Message> findLatestMessagesForConversations(@Param("conversationIds") List<Long> conversationIds);
}
