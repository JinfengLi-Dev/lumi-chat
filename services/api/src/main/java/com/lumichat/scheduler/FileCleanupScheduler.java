package com.lumichat.scheduler;

import com.lumichat.config.MinioProperties;
import com.lumichat.entity.FileEntity;
import com.lumichat.repository.FileRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to clean up expired files from storage.
 * Runs daily at 3 AM to remove files that have exceeded their retention period.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileRepository fileRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * Clean up expired files from MinIO and database.
     * Runs daily at 3 AM server time.
     */
    @Scheduled(cron = "${app.file.cleanup-cron:0 0 3 * * ?}")
    public void cleanupExpiredFiles() {
        log.info("Starting scheduled file cleanup task");

        LocalDateTime now = LocalDateTime.now();
        List<FileEntity> expiredFiles = fileRepository.findExpiredFiles(now);

        if (expiredFiles.isEmpty()) {
            log.info("No expired files to clean up");
            return;
        }

        log.info("Found {} expired files to clean up", expiredFiles.size());

        int successCount = 0;
        int failCount = 0;

        for (FileEntity file : expiredFiles) {
            try {
                // Delete main file from MinIO
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(file.getBucket())
                        .object(file.getPath())
                        .build());

                // Delete thumbnail if exists
                if (file.getThumbnailPath() != null) {
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketThumbnails())
                                .object(file.getThumbnailPath())
                                .build());
                    } catch (Exception e) {
                        log.warn("Failed to delete thumbnail for file {}: {}",
                                file.getFileId(), e.getMessage());
                    }
                }

                // Delete from database
                fileRepository.delete(file);
                successCount++;

                log.debug("Cleaned up expired file: {} (bucket: {}, path: {})",
                        file.getFileId(), file.getBucket(), file.getPath());

            } catch (Exception e) {
                failCount++;
                log.error("Failed to cleanup file {} (bucket: {}, path: {}): {}",
                        file.getFileId(), file.getBucket(), file.getPath(), e.getMessage());
            }
        }

        log.info("File cleanup completed. Success: {}, Failed: {}", successCount, failCount);
    }
}
