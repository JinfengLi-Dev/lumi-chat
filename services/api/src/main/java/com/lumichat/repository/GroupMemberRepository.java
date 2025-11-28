package com.lumichat.repository;

import com.lumichat.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId")
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.id = :userId")
    List<GroupMember> findByUserId(@Param("userId") Long userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    Optional<GroupMember> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END FROM GroupMember gm " +
           "WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.role = 'owner'")
    Optional<GroupMember> findOwnerByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.role IN ('owner', 'admin')")
    List<GroupMember> findAdminsByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
    int countByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE GroupMember gm SET gm.nickname = :nickname WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    int updateNickname(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("nickname") String nickname);

    @Modifying
    @Query("UPDATE GroupMember gm SET gm.role = :role WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    int updateRole(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("role") GroupMember.MemberRole role);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    int deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId")
    int deleteAllByGroupId(@Param("groupId") Long groupId);
}
