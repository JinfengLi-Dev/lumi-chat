package com.lumichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketAvatars;
    private String bucketImages;
    private String bucketFiles;
    private String bucketVoice;
    private String bucketVideo;
    private String bucketThumbnails;
}
