package com.lumichat.repository;

import com.lumichat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByMsgId(String msgId);

    Page<Message> findByConversationIdAndIsDeletedFalseOrderByServerCreatedAtDesc(
            Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false AND m.id < :beforeId ORDER BY m.serverCreatedAt DESC")
    Page<Message> findBeforeId(Long conversationId, Long beforeId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.serverCreatedAt > :since AND m.isDeleted = false ORDER BY m.serverCreatedAt ASC")
    List<Message> findNewMessages(Long conversationId, LocalDateTime since);
}
