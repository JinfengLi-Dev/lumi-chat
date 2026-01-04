package com.lumichat.service;

import com.lumichat.dto.request.ChangePasswordRequest;
import com.lumichat.dto.request.UpdateProfileRequest;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.entity.User;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.NotFoundException;
import com.lumichat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uid("LC12345678")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("TestUser")
                .gender(User.Gender.male)
                .status(User.UserStatus.active)
                .signature("Hello World")
                .phone("1234567890")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GetUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserByIdSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.getUserById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID")
        void shouldThrowExceptionWhenUserNotFoundById() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("GetUserByUid Tests")
    class GetUserByUidTests {

        @Test
        @DisplayName("Should get user by UID successfully")
        void shouldGetUserByUidSuccessfully() {
            // Given
            when(userRepository.findByUid("LC12345678")).thenReturn(Optional.of(testUser));

            // When
            User result = userService.getUserByUid("LC12345678");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUid()).isEqualTo("LC12345678");
        }

        @Test
        @DisplayName("Should throw exception when user not found by UID")
        void shouldThrowExceptionWhenUserNotFoundByUid() {
            // Given
            when(userRepository.findByUid("LC99999999")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getUserByUid("LC99999999"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("GetCurrentUser Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user profile successfully")
        void shouldGetCurrentUserSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getCurrentUser(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getNickname()).isEqualTo("TestUser");
        }
    }

    @Nested
    @DisplayName("UpdateProfile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update all profile fields")
        void shouldUpdateAllProfileFields() {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("NewNickname");
            request.setGender("female");
            request.setSignature("New signature");
            request.setPhone("9876543210");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserResponse result = userService.updateProfile(1L, request);

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getNickname()).isEqualTo("NewNickname");
            assertThat(savedUser.getGender()).isEqualTo(User.Gender.female);
            assertThat(savedUser.getSignature()).isEqualTo("New signature");
            assertThat(savedUser.getPhone()).isEqualTo("9876543210");
        }

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("NewNickname");
            // Other fields are null

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            userService.updateProfile(1L, request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getNickname()).isEqualTo("NewNickname");
            assertThat(savedUser.getGender()).isEqualTo(User.Gender.male); // Unchanged
            assertThat(savedUser.getSignature()).isEqualTo("Hello World"); // Unchanged
        }

        @Test
        @DisplayName("Should ignore blank nickname")
        void shouldIgnoreBlankNickname() {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("   ");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            userService.updateProfile(1L, request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getNickname()).isEqualTo("TestUser"); // Unchanged
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid gender")
        void shouldThrowBadRequestExceptionForInvalidGender() {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setGender("invalid");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then - should throw BadRequestException for invalid gender value
            assertThatThrownBy(() -> userService.updateProfile(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid gender value: invalid")
                    .hasMessageContaining("Valid values are: male, female, unknown");

            // Verify save was NOT called since validation failed
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("UpdateAvatar Tests")
    class UpdateAvatarTests {

        @Test
        @DisplayName("Should update avatar successfully")
        void shouldUpdateAvatarSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserResponse result = userService.updateAvatar(1L, "http://example.com/avatar.jpg");

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getAvatar()).isEqualTo("http://example.com/avatar.jpg");
        }
    }

    @Nested
    @DisplayName("ChangePassword Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("password123");
            request.setNewPassword("newPassword456");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("newHashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            userService.changePassword(1L, request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPasswordHash()).isEqualTo("newHashedPassword");
        }

        @Test
        @DisplayName("Should throw exception for incorrect current password")
        void shouldThrowExceptionForIncorrectCurrentPassword() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("newPassword456");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Current password is incorrect");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("SearchUsers Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users successfully")
        void shouldSearchUsersSuccessfully() {
            // Given
            User user1 = User.builder()
                    .id(2L)
                    .uid("LC11111111")
                    .email("user1@example.com")
                    .nickname("User1")
                    .status(User.UserStatus.active)
                    .build();

            User user2 = User.builder()
                    .id(3L)
                    .uid("LC22222222")
                    .email("user2@example.com")
                    .nickname("User2")
                    .status(User.UserStatus.active)
                    .build();

            when(userRepository.findByEmailContainingIgnoreCaseOrUidContainingIgnoreCase("user", "user"))
                    .thenReturn(Arrays.asList(user1, user2, testUser));

            // When
            List<UserResponse> results = userService.searchUsers("user", 1L);

            // Then
            assertThat(results).hasSize(2); // Excludes current user (testUser with id=1)
            assertThat(results).extracting(UserResponse::getId).containsExactly(2L, 3L);
        }

        @Test
        @DisplayName("Should exclude inactive users from search results")
        void shouldExcludeInactiveUsers() {
            // Given
            User activeUser = User.builder()
                    .id(2L)
                    .uid("LC11111111")
                    .email("user1@example.com")
                    .nickname("User1")
                    .status(User.UserStatus.active)
                    .build();

            User inactiveUser = User.builder()
                    .id(3L)
                    .uid("LC22222222")
                    .email("user2@example.com")
                    .nickname("User2")
                    .status(User.UserStatus.inactive)
                    .build();

            when(userRepository.findByEmailContainingIgnoreCaseOrUidContainingIgnoreCase("user", "user"))
                    .thenReturn(Arrays.asList(activeUser, inactiveUser));

            // When
            List<UserResponse> results = userService.searchUsers("user", 1L);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should throw exception for query too short")
        void shouldThrowExceptionForQueryTooShort() {
            // When/Then
            assertThatThrownBy(() -> userService.searchUsers("a", 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Search query must be at least 2 characters");
        }

        @Test
        @DisplayName("Should throw exception for null query")
        void shouldThrowExceptionForNullQuery() {
            // When/Then
            assertThatThrownBy(() -> userService.searchUsers(null, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Search query must be at least 2 characters");
        }

        @Test
        @DisplayName("Should limit results to 20")
        void shouldLimitResultsTo20() {
            // Given
            List<User> manyUsers = new java.util.ArrayList<>();
            for (int i = 2; i <= 30; i++) {
                manyUsers.add(User.builder()
                        .id((long) i)
                        .uid("LC" + String.format("%08d", i))
                        .email("user" + i + "@example.com")
                        .nickname("User" + i)
                        .status(User.UserStatus.active)
                        .build());
            }

            when(userRepository.findByEmailContainingIgnoreCaseOrUidContainingIgnoreCase("user", "user"))
                    .thenReturn(manyUsers);

            // When
            List<UserResponse> results = userService.searchUsers("user", 1L);

            // Then
            assertThat(results).hasSize(20);
        }

        @Test
        @DisplayName("Should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            when(userRepository.findByEmailContainingIgnoreCaseOrUidContainingIgnoreCase("xyz", "xyz"))
                    .thenReturn(Collections.emptyList());

            // When
            List<UserResponse> results = userService.searchUsers("xyz", 1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("GetUserResponseByUid Tests")
    class GetUserResponseByUidTests {

        @Test
        @DisplayName("Should get user response by UID successfully")
        void shouldGetUserResponseByUidSuccessfully() {
            // Given
            when(userRepository.findByUid("LC12345678")).thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getUserResponseByUid("LC12345678");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUid()).isEqualTo("LC12345678");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            // Given
            testUser.setStatus(User.UserStatus.inactive);
            when(userRepository.findByUid("LC12345678")).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> userService.getUserResponseByUid("LC12345678"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }
}
