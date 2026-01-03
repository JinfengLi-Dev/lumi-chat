package com.lumichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.request.LoginRequest;
import com.lumichat.entity.FileEntity;
import com.lumichat.entity.User;
import com.lumichat.repository.FileRepository;
import com.lumichat.repository.UserDeviceRepository;
import com.lumichat.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileController.
 * Uses MockBean for MinioClient since it requires an external MinIO server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MinioClient minioClient;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        fileRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("USER001", "user1@example.com", "User One");
        user2 = createUser("USER002", "user2@example.com", "User Two");

        user1Token = loginAndGetToken("user1@example.com", "device-user1");
        user2Token = loginAndGetToken("user2@example.com", "device-user2");

        // Default mock for bucket exists
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    }

    private User createUser(String uid, String email, String nickname) {
        User user = User.builder()
                .uid(uid)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname(nickname)
                .gender(User.Gender.male)
                .status(User.UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private String loginAndGetToken(String email, String deviceId) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setDeviceId(deviceId);
        request.setDeviceType("web");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    private FileEntity createFileEntity(User owner, String fileId, String originalName, String mimeType) {
        FileEntity file = FileEntity.builder()
                .fileId(fileId)
                .uploader(owner)
                .originalName(originalName)
                .storedName(fileId + getExtension(originalName))
                .mimeType(mimeType)
                .sizeBytes(1024L)
                .bucket("images")
                .path(fileId + getExtension(originalName))
                .createdAt(LocalDateTime.now())
                .build();
        return fileRepository.save(file);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }

    @Nested
    @DisplayName("POST /files/upload")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload image file successfully")
        void shouldUploadImageFileSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-image.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            mockMvc.perform(multipart("/files/upload")
                            .file(file)
                            .param("type", "image")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileName").value("test-image.jpg"))
                    .andExpect(jsonPath("$.data.mimeType").value("image/jpeg"))
                    .andExpect(jsonPath("$.data.fileId").isNotEmpty());

            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should upload PDF file successfully")
        void shouldUploadPdfFileSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "pdf content".getBytes()
            );

            mockMvc.perform(multipart("/files/upload")
                            .file(file)
                            .param("type", "file")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileName").value("document.pdf"))
                    .andExpect(jsonPath("$.data.mimeType").value("application/pdf"));
        }

        @Test
        @DisplayName("Should fail with empty file")
        void shouldFailWithEmptyFile() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.txt",
                    "text/plain",
                    new byte[0]
            );

            mockMvc.perform(multipart("/files/upload")
                            .file(emptyFile)
                            .param("type", "file")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    "content".getBytes()
            );

            mockMvc.perform(multipart("/files/upload")
                            .file(file)
                            .param("type", "image"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /files/avatar")
    class UploadAvatarTests {

        @Test
        @DisplayName("Should upload avatar successfully")
        void shouldUploadAvatarSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "avatar.png",
                    "image/png",
                    "avatar content".getBytes()
            );

            mockMvc.perform(multipart("/files/avatar")
                            .file(file)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileName").value("avatar.png"));

            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should fail with empty avatar file")
        void shouldFailWithEmptyAvatarFile() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.png",
                    "image/png",
                    new byte[0]
            );

            mockMvc.perform(multipart("/files/avatar")
                            .file(emptyFile)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /files/{id}")
    class GetFileTests {

        @Test
        @DisplayName("Should get file content with inline disposition")
        void shouldGetFileContentWithInlineDisposition() throws Exception {
            FileEntity file = createFileEntity(user1, "file123", "image.jpg", "image/jpeg");

            // Mock MinIO getObject
            byte[] fileContent = "test image content".getBytes();
            GetObjectResponse mockResponse = mock(GetObjectResponse.class);
            when(mockResponse.read(any(byte[].class))).thenAnswer(inv -> {
                byte[] buffer = inv.getArgument(0);
                System.arraycopy(fileContent, 0, buffer, 0, Math.min(buffer.length, fileContent.length));
                return fileContent.length;
            }).thenReturn(-1);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

            mockMvc.perform(get("/files/" + file.getFileId()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("inline")))
                    .andExpect(header().string("Content-Disposition", containsString("image.jpg")));
        }

        @Test
        @DisplayName("Should fail when file not found")
        void shouldFailWhenFileNotFound() throws Exception {
            mockMvc.perform(get("/files/nonexistent-file-id"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /files/{id}/download")
    class DownloadFileTests {

        @Test
        @DisplayName("Should download file with attachment disposition")
        void shouldDownloadFileWithAttachmentDisposition() throws Exception {
            FileEntity file = createFileEntity(user1, "file456", "document.pdf", "application/pdf");

            // Mock MinIO getObject
            byte[] fileContent = "pdf content".getBytes();
            GetObjectResponse mockResponse = mock(GetObjectResponse.class);
            when(mockResponse.read(any(byte[].class))).thenAnswer(inv -> {
                byte[] buffer = inv.getArgument(0);
                System.arraycopy(fileContent, 0, buffer, 0, Math.min(buffer.length, fileContent.length));
                return fileContent.length;
            }).thenReturn(-1);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

            mockMvc.perform(get("/files/" + file.getFileId() + "/download"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", containsString("document.pdf")))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM));
        }
    }

    @Nested
    @DisplayName("GET /files/{id}/info")
    class GetFileInfoTests {

        @Test
        @DisplayName("Should get file info successfully")
        void shouldGetFileInfoSuccessfully() throws Exception {
            FileEntity file = createFileEntity(user1, "file789", "photo.jpg", "image/jpeg");

            mockMvc.perform(get("/files/" + file.getFileId() + "/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileId").value("file789"))
                    .andExpect(jsonPath("$.data.fileName").value("photo.jpg"))
                    .andExpect(jsonPath("$.data.mimeType").value("image/jpeg"));
        }

        @Test
        @DisplayName("Should fail when file info not found")
        void shouldFailWhenFileInfoNotFound() throws Exception {
            mockMvc.perform(get("/files/nonexistent/info"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /files")
    class GetUserFilesTests {

        @Test
        @DisplayName("Should get all user files")
        void shouldGetAllUserFiles() throws Exception {
            createFileEntity(user1, "file1", "image1.jpg", "image/jpeg");
            createFileEntity(user1, "file2", "image2.png", "image/png");
            createFileEntity(user2, "file3", "other.jpg", "image/jpeg"); // Different user

            mockMvc.perform(get("/files")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Should get user files by type")
        void shouldGetUserFilesByType() throws Exception {
            createFileEntity(user1, "img1", "photo.jpg", "image/jpeg");

            mockMvc.perform(get("/files")
                            .param("type", "image")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should return empty list when no files")
        void shouldReturnEmptyListWhenNoFiles() throws Exception {
            mockMvc.perform(get("/files")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get("/files"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /files/{id}")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete own file successfully")
        void shouldDeleteOwnFileSuccessfully() throws Exception {
            FileEntity file = createFileEntity(user1, "delete-me", "todelete.jpg", "image/jpeg");

            mockMvc.perform(delete("/files/" + file.getFileId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should fail when deleting other's file")
        void shouldFailWhenDeletingOthersFile() throws Exception {
            FileEntity file = createFileEntity(user2, "not-mine", "other.jpg", "image/jpeg");

            mockMvc.perform(delete("/files/" + file.getFileId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail when file not found")
        void shouldFailWhenFileNotFound() throws Exception {
            mockMvc.perform(delete("/files/nonexistent-file")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            FileEntity file = createFileEntity(user1, "auth-test", "test.jpg", "image/jpeg");

            mockMvc.perform(delete("/files/" + file.getFileId()))
                    .andExpect(status().isForbidden());
        }
    }
}
