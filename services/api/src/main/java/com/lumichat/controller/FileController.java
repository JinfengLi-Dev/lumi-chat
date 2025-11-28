package com.lumichat.controller;

import com.lumichat.dto.response.ApiResponse;
import com.lumichat.dto.response.FileResponse;
import com.lumichat.entity.FileEntity;
import com.lumichat.security.UserPrincipal;
import com.lumichat.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload a file
     * POST /files/upload
     */
    @PostMapping("/upload")
    public ApiResponse<FileResponse> uploadFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "file") String type) {
        FileResponse response = fileStorageService.uploadFile(principal.getId(), file, type);
        return ApiResponse.success(response);
    }

    /**
     * Upload an avatar
     * POST /files/avatar
     */
    @PostMapping("/avatar")
    public ApiResponse<FileResponse> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        FileResponse response = fileStorageService.uploadAvatar(principal.getId(), file);
        return ApiResponse.success(response);
    }

    /**
     * Get file info
     * GET /files/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String id) {
        FileEntity fileEntity = fileStorageService.getFileEntity(id);
        InputStream inputStream = fileStorageService.getFileContent(id);

        String contentType = fileEntity.getMimeType() != null
                ? fileEntity.getMimeType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileEntity.getFileName() + "\"")
                .body(new InputStreamResource(inputStream));
    }

    /**
     * Download file
     * GET /files/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String id) {
        FileEntity fileEntity = fileStorageService.getFileEntity(id);
        InputStream inputStream = fileStorageService.getFileContent(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                .body(new InputStreamResource(inputStream));
    }

    /**
     * Get file metadata
     * GET /files/{id}/info
     */
    @GetMapping("/{id}/info")
    public ApiResponse<FileResponse> getFileInfo(@PathVariable String id) {
        FileResponse response = fileStorageService.getFile(id);
        return ApiResponse.success(response);
    }

    /**
     * Get user's files
     * GET /files
     */
    @GetMapping
    public ApiResponse<List<FileResponse>> getUserFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type) {
        List<FileResponse> files = fileStorageService.getUserFiles(principal.getId(), type);
        return ApiResponse.success(files);
    }

    /**
     * Delete file
     * DELETE /files/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        fileStorageService.deleteFile(principal.getId(), id);
        return ApiResponse.success();
    }
}
