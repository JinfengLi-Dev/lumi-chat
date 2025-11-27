package com.lumichat.repository;

import com.lumichat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUid(String uid);

    boolean existsByEmail(String email);

    boolean existsByUid(String uid);

    Optional<User> findByEmailOrUid(String email, String uid);
}
