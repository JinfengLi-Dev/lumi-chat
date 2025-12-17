package com.lumichat.exception;

import com.lumichat.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests exception handling and Unicode message sanitization.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("NotFoundException handling")
    class NotFoundExceptionTests {
        @Test
        @DisplayName("should return 404 with error message")
        void shouldReturn404WithMessage() {
            NotFoundException ex = new NotFoundException("User not found");
            ResponseEntity<ApiResponse<Void>> response = handler.handleNotFoundException(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getCode());
            assertEquals("User not found", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("BadRequestException handling")
    class BadRequestExceptionTests {
        @Test
        @DisplayName("should return 400 with error message")
        void shouldReturn400WithMessage() {
            BadRequestException ex = new BadRequestException("Invalid input");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequestException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getCode());
            assertEquals("Invalid input", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("UnauthorizedException handling")
    class UnauthorizedExceptionTests {
        @Test
        @DisplayName("should return 401 with error message")
        void shouldReturn401WithMessage() {
            UnauthorizedException ex = new UnauthorizedException("Invalid credentials");
            ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorizedException(ex);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getCode());
            assertEquals("Invalid credentials", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("ForbiddenException handling")
    class ForbiddenExceptionTests {
        @Test
        @DisplayName("should return 403 with error message")
        void shouldReturn403WithMessage() {
            ForbiddenException ex = new ForbiddenException("Access denied");
            ResponseEntity<ApiResponse<Void>> response = handler.handleForbiddenException(ex);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getCode());
            assertEquals("Access denied", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("RuntimeException handling with Unicode sanitization")
    class RuntimeExceptionTests {
        @Test
        @DisplayName("should return ASCII message unchanged")
        void shouldReturnAsciiMessageUnchanged() {
            RuntimeException ex = new RuntimeException("Simple ASCII error message");
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Simple ASCII error message", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should sanitize Chinese characters in message")
        void shouldSanitizeChineseCharacters() {
            RuntimeException ex = new RuntimeException("Error at 微软大厦, Beijing");
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            // Should return generic message when non-ASCII detected
            assertEquals("Request failed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should sanitize Japanese characters in message")
        void shouldSanitizeJapaneseCharacters() {
            RuntimeException ex = new RuntimeException("エラーが発生しました");
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Request failed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should sanitize Korean characters in message")
        void shouldSanitizeKoreanCharacters() {
            RuntimeException ex = new RuntimeException("오류가 발생했습니다");
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Request failed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should sanitize emoji in message")
        void shouldSanitizeEmoji() {
            RuntimeException ex = new RuntimeException("Error 😀 occurred");
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Request failed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should handle null message")
        void shouldHandleNullMessage() {
            RuntimeException ex = new RuntimeException((String) null);
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Request failed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should handle empty message")
        void shouldHandleEmptyMessage() {
            RuntimeException ex = new RuntimeException("");
            ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException handling")
    class IllegalArgumentExceptionTests {
        @Test
        @DisplayName("should return generic message for all cases")
        void shouldReturnGenericMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("The Unicode character [微] cannot be encoded");
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            // Always returns generic message to prevent Unicode issues
            assertEquals("Invalid request parameters", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should return generic message even for ASCII error")
        void shouldReturnGenericMessageForAscii() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter value");
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Invalid request parameters", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Generic Exception handling")
    class GenericExceptionTests {
        @Test
        @DisplayName("should return 500 with generic message")
        void shouldReturn500WithGenericMessage() {
            Exception ex = new Exception("Some internal error");
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should not expose Unicode in error message")
        void shouldNotExposeUnicodeInError() {
            Exception ex = new Exception("数据库错误");
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            // Generic handler always returns safe message
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }
    }
}
