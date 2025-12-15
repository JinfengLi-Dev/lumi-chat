package com.lumichat.dto.response;

import com.lumichat.entity.FileEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileResponse {
    private String fileId;
    private String fileName;
    private Long fileSize;
    private String bucket;
    private String mimeType;
    private String url;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private Integer duration;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String fileType;

    public static FileResponse from(FileEntity file, String baseUrl) {
        return FileResponse.builder()
                .fileId(file.getFileId())
                .fileName(file.getOriginalName())
                .fileSize(file.getSizeBytes())
                .bucket(file.getBucket())
                .mimeType(file.getMimeType())
                .url(baseUrl + "/files/" + file.getFileId())
                .thumbnailUrl(file.getThumbnailPath() != null
                        ? baseUrl + "/files/" + file.getFileId() + "/thumbnail"
                        : null)
                .width(file.getWidth())
                .height(file.getHeight())
                .duration(file.getDuration())
                .createdAt(file.getCreatedAt())
                .expiresAt(file.getExpiresAt())
                .fileType(file.getFileType())
                .build();
    }
}
