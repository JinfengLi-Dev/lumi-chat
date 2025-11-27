package com.lumichat.repository;

import com.lumichat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.type = 'private_chat' AND c.participantIds LIKE %:userId% ORDER BY c.id")
    Optional<Conversation> findPrivateChat(Long userId, Long otherUserId);
}
