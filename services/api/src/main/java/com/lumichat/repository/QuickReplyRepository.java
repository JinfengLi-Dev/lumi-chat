package com.lumichat.repository;

import com.lumichat.entity.QuickReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuickReplyRepository extends JpaRepository<QuickReply, Long> {

    List<QuickReply> findByUserIdOrderBySortOrderAsc(Long userId);

    int countByUserId(Long userId);

    Optional<QuickReply> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM QuickReply q WHERE q.id = :id AND q.user.id = :userId")
    int deleteByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(MAX(q.sortOrder), 0) FROM QuickReply q WHERE q.user.id = :userId")
    int findMaxSortOrderByUserId(Long userId);
}
