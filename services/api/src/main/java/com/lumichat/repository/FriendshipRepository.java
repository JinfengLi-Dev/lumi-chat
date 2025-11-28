package com.lumichat.repository;

import com.lumichat.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :userId AND f.status = 'active'")
    List<Friendship> findByUserIdAndActive(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :userId")
    List<Friendship> findByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :userId AND f.friend.id = :friendId")
    Optional<Friendship> findByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE f.user.id = :userId AND f.friend.id = :friendId")
    boolean existsByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Modifying
    @Query("UPDATE Friendship f SET f.remark = :remark WHERE f.user.id = :userId AND f.friend.id = :friendId")
    int updateRemark(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("remark") String remark);

    @Modifying
    @Query("UPDATE Friendship f SET f.status = :status WHERE f.user.id = :userId AND f.friend.id = :friendId")
    int updateStatus(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.user.id = :userId AND f.friend.id = :friendId")
    int deleteByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
