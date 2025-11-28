package com.lumichat.repository;

import com.lumichat.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByFileId(String fileId);

    boolean existsByFileId(String fileId);

    @Query("SELECT f FROM FileEntity f WHERE f.uploader.id = :userId ORDER BY f.createdAt DESC")
    List<FileEntity> findByUploaderId(@Param("userId") Long userId);

    @Query("SELECT f FROM FileEntity f WHERE f.uploader.id = :userId AND f.fileType = :fileType ORDER BY f.createdAt DESC")
    List<FileEntity> findByUploaderIdAndFileType(@Param("userId") Long userId, @Param("fileType") String fileType);
}
