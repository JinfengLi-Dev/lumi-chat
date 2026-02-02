package com.lumichat.repository;

import com.lumichat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUid(String uid);

    boolean existsByEmail(String email);

    boolean existsByUid(String uid);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmailOrUid(String email, String uid);

    List<User> findByEmailContainingIgnoreCaseOrUidContainingIgnoreCase(String email, String uid);
}
