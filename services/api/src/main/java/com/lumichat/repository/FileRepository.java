package com.lumichat.repository;

import com.lumichat.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByFileId(String fileId);

    boolean existsByFileId(String fileId);

    @Query("SELECT f FROM FileEntity f WHERE f.uploader.id = :userId ORDER BY f.createdAt DESC")
    List<FileEntity> findByUploaderId(@Param("userId") Long userId);

    @Query("SELECT f FROM FileEntity f WHERE f.uploader.id = :userId AND f.bucket = :bucket ORDER BY f.createdAt DESC")
    List<FileEntity> findByUploaderIdAndBucket(@Param("userId") Long userId, @Param("bucket") String bucket);

    @Query("SELECT f FROM FileEntity f WHERE f.expiresAt IS NOT NULL AND f.expiresAt < :now")
    List<FileEntity> findExpiredFiles(@Param("now") LocalDateTime now);
}
