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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Maximum file size in bytes (10 MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Maximum avatar size in bytes (5 MB)
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    // Dangerous file extensions that could be executed
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar",
            "msi", "dll", "com", "scr", "pif", "hta", "php", "asp",
            "aspx", "jsp", "cgi", "pl", "py", "rb", "class"
    );

    // Map of MIME types to expected extensions for validation
    private static final Map<String, Set<String>> MIME_TO_EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", Set.of("jpg", "jpeg")),
            Map.entry("image/png", Set.of("png")),
            Map.entry("image/gif", Set.of("gif")),
            Map.entry("image/webp", Set.of("webp")),
            Map.entry("image/svg+xml", Set.of("svg")),
            Map.entry("video/mp4", Set.of("mp4")),
            Map.entry("video/webm", Set.of("webm")),
            Map.entry("audio/mpeg", Set.of("mp3")),
            Map.entry("audio/ogg", Set.of("ogg")),
            Map.entry("audio/wav", Set.of("wav")),
            Map.entry("application/pdf", Set.of("pdf")),
            Map.entry("text/plain", Set.of("txt")),
            Map.entry("application/msword", Set.of("doc")),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")),
            Map.entry("application/vnd.ms-excel", Set.of("xls")),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Set.of("xlsx")),
            Map.entry("application/zip", Set.of("zip")),
            Map.entry("application/x-rar-compressed", Set.of("rar"))
    );

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

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 10 MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        // Security: Validate file to prevent attacks
        validateFileUpload(originalFilename, extension, file.getContentType());

        String bucket = getBucketForFileType(fileType);
        String fileId = generateFileId();
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
                    .originalName(originalFilename)
                    .storedName(storagePath)
                    .sizeBytes(file.getSize())
                    .bucket(bucket)
                    .path(storagePath)
                    .mimeType(file.getContentType())
                    .fileType(fileType)
                    .expiresAt(calculateExpiration(fileType))
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
        // Avatar-specific validation (stricter than general file upload)
        validateAvatarUpload(file);
        return uploadFile(userId, file, "avatar");
    }

    /**
     * Upload voice file (for voice messages and voice introductions)
     */
    @Transactional
    public FileResponse uploadVoice(Long userId, MultipartFile file) {
        // Voice-specific validation
        validateVoiceUpload(file);
        return uploadFile(userId, file, "voice");
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

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(fileEntity.getBucket())
                            .object(fileEntity.getPath())
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
        List<FileEntity> files;
        if (fileType != null) {
            String bucket = getBucketForFileType(fileType);
            files = fileRepository.findByUploaderIdAndBucket(userId, bucket);
        } else {
            files = fileRepository.findByUploaderId(userId);
        }
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

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(fileEntity.getBucket())
                            .object(fileEntity.getPath())
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", e.getMessage());
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
        // Return relative URL - frontend proxy handles routing to backend
        // This ensures URLs work from any host (localhost, production, other users)
        return contextPath;
    }

    /**
     * Calculate expiration date based on file type.
     * Avatars and images don't expire, other files expire after 30 days.
     */
    private LocalDateTime calculateExpiration(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "avatar", "image", "thumbnail" -> null;  // Don't expire
            default -> LocalDateTime.now().plusDays(30);  // 30-day retention
        };
    }

    /**
     * Validate file upload for security.
     * Checks for dangerous extensions, double extensions, and MIME type mismatches.
     */
    private void validateFileUpload(String filename, String extension, String contentType) {
        if (filename == null) {
            throw new BadRequestException("Filename is required");
        }

        // Check for dangerous extensions
        if (extension != null && DANGEROUS_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("Blocked dangerous file extension upload: {}", filename);
            throw new BadRequestException("File type not allowed: " + extension);
        }

        // Check for double extensions (e.g., malware.php.jpg)
        // These can be used to bypass security on some servers
        String[] parts = filename.split("\\.");
        if (parts.length > 2) {
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i].toLowerCase();
                if (DANGEROUS_EXTENSIONS.contains(part)) {
                    log.warn("Blocked double extension attack: {}", filename);
                    throw new BadRequestException("Invalid filename format");
                }
            }
        }

        // Validate MIME type matches extension (if we know the MIME type)
        if (contentType != null && extension != null) {
            Set<String> expectedExtensions = MIME_TO_EXTENSIONS.get(contentType.toLowerCase());
            if (expectedExtensions != null && !expectedExtensions.contains(extension.toLowerCase())) {
                log.warn("MIME type mismatch: {} vs extension {}", contentType, extension);
                throw new BadRequestException("File extension does not match content type");
            }
        }
    }

    /**
     * Validate avatar upload specifically.
     * More restrictive than general file upload - only allows images.
     */
    private void validateAvatarUpload(MultipartFile file) {
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("Avatar size exceeds maximum allowed size of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Avatar must be an image file");
        }

        // Specifically check for allowed image types
        Set<String> allowedImageTypes = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (!allowedImageTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Avatar must be JPEG, PNG, GIF, or WebP format");
        }
    }

    /**
     * Validate voice upload specifically.
     * Only allows audio files.
     */
    private void validateVoiceUpload(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Voice file size exceeds maximum allowed size of 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new BadRequestException("Voice file must be an audio file");
        }

        // Specifically check for allowed audio types
        Set<String> allowedAudioTypes = Set.of(
            "audio/mpeg", "audio/mp3", "audio/ogg", "audio/wav", "audio/webm",
            "audio/m4a", "audio/aac", "audio/x-m4a"
        );
        if (!allowedAudioTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Voice file must be MP3, OGG, WAV, WebM, M4A, or AAC format");
        }
    }
}
