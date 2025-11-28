package com.lumichat.repository;

import com.lumichat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query(value = "SELECT * FROM conversations c WHERE c.type = 'private_chat' AND c.participant_ids @> ARRAY[:userId, :otherUserId]::bigint[] LIMIT 1", nativeQuery = true)
    Optional<Conversation> findPrivateChat(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
