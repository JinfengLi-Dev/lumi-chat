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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    private User testUser;
    private FileEntity testFile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .nickname("TestUser")
                .build();

        testFile = FileEntity.builder()
                .id(1L)
                .fileId("abc123def456")
                .uploader(testUser)
                .originalName("test-image.jpg")
                .storedName("abc123def456.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(1024L)
                .bucket("images")
                .path("abc123def456.jpg")
                .createdAt(LocalDateTime.now())
                .build();

        // Set @Value fields
        ReflectionTestUtils.setField(fileStorageService, "contextPath", "/api/v1");
        ReflectionTestUtils.setField(fileStorageService, "serverPort", 8080);
    }

    @Nested
    @DisplayName("UploadFile Tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload image file successfully")
        void shouldUploadImageFileSuccessfully() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "test-image.jpg",
                    "image/jpeg",
                    "test content".getBytes()
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketImages()).thenReturn("images");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                f.setCreatedAt(LocalDateTime.now());
                return f;
            });

            // When
            FileResponse result = fileStorageService.uploadFile(1L, mockFile, "image");

            // Then
            assertThat(result).isNotNull();

            verify(minioClient).putObject(any(PutObjectArgs.class));
            verify(fileRepository).save(any(FileEntity.class));
        }

        @Test
        @DisplayName("Should upload avatar file successfully")
        void shouldUploadAvatarFileSuccessfully() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "avatar.png",
                    "image/png",
                    "avatar content".getBytes()
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketAvatars()).thenReturn("avatars");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                f.setCreatedAt(LocalDateTime.now());
                return f;
            });

            // When
            FileResponse result = fileStorageService.uploadAvatar(1L, mockFile);

            // Then
            assertThat(result).isNotNull();
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw exception for empty file")
        void shouldThrowExceptionForEmptyFile() {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.txt",
                    "text/plain",
                    new byte[0]
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> fileStorageService.uploadFile(1L, emptyFile, "file"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("File is empty");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "content".getBytes()
            );

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileStorageService.uploadFile(999L, mockFile, "file"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should create bucket if not exists")
        void shouldCreateBucketIfNotExists() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "pdf content".getBytes()
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketFiles()).thenReturn("files");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                f.setCreatedAt(LocalDateTime.now());
                return f;
            });

            // When
            fileStorageService.uploadFile(1L, mockFile, "file");

            // Then
            verify(minioClient).makeBucket(any(MakeBucketArgs.class));
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should handle file without extension")
        void shouldHandleFileWithoutExtension() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "noextension",
                    "application/octet-stream",
                    "content".getBytes()
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketFiles()).thenReturn("files");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                f.setCreatedAt(LocalDateTime.now());
                return f;
            });

            // When
            FileResponse result = fileStorageService.uploadFile(1L, mockFile, "file");

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("GetFile Tests")
    class GetFileTests {

        @Test
        @DisplayName("Should get file by ID successfully")
        void shouldGetFileByIdSuccessfully() {
            // Given
            when(fileRepository.findByFileId("abc123def456")).thenReturn(Optional.of(testFile));

            // When
            FileResponse result = fileStorageService.getFile("abc123def456");

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void shouldThrowExceptionWhenFileNotFound() {
            // Given
            when(fileRepository.findByFileId("notfound")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileStorageService.getFile("notfound"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("File not found");
        }
    }

    @Nested
    @DisplayName("GetFileContent Tests")
    class GetFileContentTests {

        @Test
        @DisplayName("Should throw exception when minio fails")
        void shouldThrowExceptionWhenMinioFails() throws Exception {
            // Given
            when(fileRepository.findByFileId("abc123def456")).thenReturn(Optional.of(testFile));
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new RuntimeException("MinIO connection failed"));

            // When/Then
            assertThatThrownBy(() -> fileStorageService.getFileContent("abc123def456"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Failed to retrieve file");
        }

        @Test
        @DisplayName("Should throw exception when file not found for content")
        void shouldThrowExceptionWhenFileNotFoundForContent() {
            // Given
            when(fileRepository.findByFileId("notfound")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileStorageService.getFileContent("notfound"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("File not found");
        }
    }

    @Nested
    @DisplayName("GetFileEntity Tests")
    class GetFileEntityTests {

        @Test
        @DisplayName("Should get file entity successfully")
        void shouldGetFileEntitySuccessfully() {
            // Given
            when(fileRepository.findByFileId("abc123def456")).thenReturn(Optional.of(testFile));

            // When
            FileEntity result = fileStorageService.getFileEntity("abc123def456");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFileId()).isEqualTo("abc123def456");
        }

        @Test
        @DisplayName("Should throw exception when file entity not found")
        void shouldThrowExceptionWhenFileEntityNotFound() {
            // Given
            when(fileRepository.findByFileId("notfound")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileStorageService.getFileEntity("notfound"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("File not found");
        }
    }

    @Nested
    @DisplayName("GetUserFiles Tests")
    class GetUserFilesTests {

        @Test
        @DisplayName("Should get all user files")
        void shouldGetAllUserFiles() {
            // Given
            when(fileRepository.findByUploaderId(1L)).thenReturn(Arrays.asList(testFile));

            // When
            List<FileResponse> results = fileStorageService.getUserFiles(1L, null);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should get user files by type")
        void shouldGetUserFilesByType() {
            // Given
            when(minioProperties.getBucketImages()).thenReturn("images");
            when(fileRepository.findByUploaderIdAndBucket(1L, "images"))
                    .thenReturn(Arrays.asList(testFile));

            // When
            List<FileResponse> results = fileStorageService.getUserFiles(1L, "image");

            // Then
            assertThat(results).hasSize(1);
            verify(fileRepository).findByUploaderIdAndBucket(1L, "images");
        }

        @Test
        @DisplayName("Should return empty list when no files")
        void shouldReturnEmptyListWhenNoFiles() {
            // Given
            when(fileRepository.findByUploaderId(1L)).thenReturn(Collections.emptyList());

            // When
            List<FileResponse> results = fileStorageService.getUserFiles(1L, null);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("DeleteFile Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() throws Exception {
            // Given
            when(fileRepository.findByFileId("abc123def456")).thenReturn(Optional.of(testFile));

            // When
            fileStorageService.deleteFile(1L, "abc123def456");

            // Then
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
            verify(fileRepository).delete(testFile);
        }

        @Test
        @DisplayName("Should throw exception when deleting others file")
        void shouldThrowExceptionWhenDeletingOthersFile() {
            // Given
            User otherUser = User.builder().id(2L).build();
            testFile.setUploader(otherUser);
            when(fileRepository.findByFileId("abc123def456")).thenReturn(Optional.of(testFile));

            // When/Then
            assertThatThrownBy(() -> fileStorageService.deleteFile(1L, "abc123def456"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You can only delete your own files");
        }

        @Test
        @DisplayName("Should throw exception when file not found for delete")
        void shouldThrowExceptionWhenFileNotFoundForDelete() {
            // Given
            when(fileRepository.findByFileId("notfound")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> fileStorageService.deleteFile(1L, "notfound"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("File not found");
        }

        @Test
        @DisplayName("Should handle minio delete failure gracefully")
        void shouldHandleMinioDeleteFailureGracefully() throws Exception {
            // Given
            when(fileRepository.findByFileId("abc123def456")).thenReturn(Optional.of(testFile));
            doThrow(new RuntimeException("MinIO error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // When
            fileStorageService.deleteFile(1L, "abc123def456");

            // Then - should still delete from repository
            verify(fileRepository).delete(testFile);
        }
    }

    @Nested
    @DisplayName("Bucket Selection Tests")
    class BucketSelectionTests {

        @Test
        @DisplayName("Should select correct bucket for avatar")
        void shouldSelectCorrectBucketForAvatar() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", "content".getBytes());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketAvatars()).thenReturn("avatars");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                return f;
            });

            // When
            fileStorageService.uploadFile(1L, mockFile, "avatar");

            // Then
            ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
            verify(fileRepository).save(fileCaptor.capture());
            assertThat(fileCaptor.getValue().getBucket()).isEqualTo("avatars");
        }

        @Test
        @DisplayName("Should select correct bucket for voice")
        void shouldSelectCorrectBucketForVoice() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "voice.mp3", "audio/mp3", "content".getBytes());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketVoice()).thenReturn("voice");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                return f;
            });

            // When
            fileStorageService.uploadFile(1L, mockFile, "voice");

            // Then
            ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
            verify(fileRepository).save(fileCaptor.capture());
            assertThat(fileCaptor.getValue().getBucket()).isEqualTo("voice");
        }

        @Test
        @DisplayName("Should select correct bucket for video")
        void shouldSelectCorrectBucketForVideo() throws Exception {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "video.mp4", "video/mp4", "content".getBytes());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(minioProperties.getBucketVideo()).thenReturn("video");
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> {
                FileEntity f = inv.getArgument(0);
                f.setId(2L);
                return f;
            });

            // When
            fileStorageService.uploadFile(1L, mockFile, "video");

            // Then
            ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
            verify(fileRepository).save(fileCaptor.capture());
            assertThat(fileCaptor.getValue().getBucket()).isEqualTo("video");
        }
    }
}
