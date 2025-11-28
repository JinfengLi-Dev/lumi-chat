package com.lumichat.repository;

import com.lumichat.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.toUser.id = :userId ORDER BY fr.createdAt DESC")
    List<FriendRequest> findByToUserId(@Param("userId") Long userId);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.toUser.id = :userId AND fr.status = 'pending' ORDER BY fr.createdAt DESC")
    List<FriendRequest> findPendingByToUserId(@Param("userId") Long userId);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.fromUser.id = :userId ORDER BY fr.createdAt DESC")
    List<FriendRequest> findByFromUserId(@Param("userId") Long userId);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.fromUser.id = :fromId AND fr.toUser.id = :toId AND fr.status = 'pending'")
    Optional<FriendRequest> findPendingRequest(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Query("SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END FROM FriendRequest fr " +
           "WHERE fr.fromUser.id = :fromId AND fr.toUser.id = :toId AND fr.status = 'pending'")
    boolean existsPendingRequest(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
