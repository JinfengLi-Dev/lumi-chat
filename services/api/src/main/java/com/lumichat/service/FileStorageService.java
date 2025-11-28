package com.lumichat.service;

import com.lumichat.config.MinioProperties;
import com.lumichat.dto.response.FileResponse;
import com.lumichat.entity.FileEntity;
import com.lumichat.entity.User;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.FileRepository;
import com.lumichat.repository.UserRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Upload a file
     */
    @Transactional
    public FileResponse uploadFile(Long userId, MultipartFile file, String fileType) {
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String bucket = getBucketForFileType(fileType);
        String fileId = generateFileId();
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storagePath = fileId + (extension != null ? "." + extension : "");

        try {
            // Ensure bucket exists
            ensureBucketExists(bucket);

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Save file metadata
            FileEntity fileEntity = FileEntity.builder()
                    .fileId(fileId)
                    .uploader(uploader)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .mimeType(file.getContentType())
                    .storagePath(bucket + "/" + storagePath)
                    .build();

            fileEntity = fileRepository.save(fileEntity);
            log.info("File {} uploaded by user {} to bucket {}", fileId, userId, bucket);

            return FileResponse.from(fileEntity, getBaseUrl());
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Upload avatar
     */
    @Transactional
    public FileResponse uploadAvatar(Long userId, MultipartFile file) {
        return uploadFile(userId, file, "avatar");
    }

    /**
     * Get file by ID
     */
    public FileResponse getFile(String fileId) {
        FileEntity fileEntity = fileRepository.findByFileId(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));
        return FileResponse.from(fileEntity, getBaseUrl());
    }

    /**
     * Get file content stream
     */
    public InputStream getFileContent(String fileId) {
        FileEntity fileEntity = fileRepository.findByFileId(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        String[] pathParts = fileEntity.getStoragePath().split("/", 2);
        if (pathParts.length != 2) {
            throw new BadRequestException("Invalid storage path");
        }

        String bucket = pathParts[0];
        String objectName = pathParts[1];

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get file content", e);
            throw new BadRequestException("Failed to retrieve file: " + e.getMessage());
        }
    }

    /**
     * Get file entity for content type
     */
    public FileEntity getFileEntity(String fileId) {
        return fileRepository.findByFileId(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));
    }

    /**
     * Get user files
     */
    public List<FileResponse> getUserFiles(Long userId, String fileType) {
        List<FileEntity> files = fileType != null
                ? fileRepository.findByUploaderIdAndFileType(userId, fileType)
                : fileRepository.findByUploaderId(userId);
        String baseUrl = getBaseUrl();
        return files.stream()
                .map(f -> FileResponse.from(f, baseUrl))
                .collect(Collectors.toList());
    }

    /**
     * Delete file
     */
    @Transactional
    public void deleteFile(Long userId, String fileId) {
        FileEntity fileEntity = fileRepository.findByFileId(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        if (!fileEntity.getUploader().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own files");
        }

        String[] pathParts = fileEntity.getStoragePath().split("/", 2);
        if (pathParts.length == 2) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(pathParts[0])
                                .object(pathParts[1])
                                .build()
                );
            } catch (Exception e) {
                log.warn("Failed to delete file from MinIO: {}", e.getMessage());
            }
        }

        fileRepository.delete(fileEntity);
        log.info("File {} deleted by user {}", fileId, userId);
    }

    // Helper methods

    private String getBucketForFileType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "avatar" -> minioProperties.getBucketAvatars();
            case "image" -> minioProperties.getBucketImages();
            case "voice" -> minioProperties.getBucketVoice();
            case "video" -> minioProperties.getBucketVideo();
            case "thumbnail" -> minioProperties.getBucketThumbnails();
            default -> minioProperties.getBucketFiles();
        };
    }

    private String generateFileId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", bucket, e);
            throw new BadRequestException("Failed to initialize storage: " + e.getMessage());
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + serverPort + contextPath;
    }
}
