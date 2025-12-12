package com.lumichat.service;

import com.lumichat.dto.request.LoginRequest;
import com.lumichat.dto.request.RegisterRequest;
import com.lumichat.dto.response.LoginResponse;
import com.lumichat.dto.response.UserResponse;
import com.lumichat.entity.User;
import com.lumichat.entity.UserDevice;
import com.lumichat.exception.BadRequestException;
import com.lumichat.exception.UnauthorizedException;
import com.lumichat.repository.UserDeviceRepository;
import com.lumichat.repository.UserRepository;
import com.lumichat.security.JwtTokenProvider;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

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
                .createdAt(LocalDateTime.now())
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setDeviceId("device-123");
        loginRequest.setDeviceType("web");
        loginRequest.setDeviceName("Chrome Browser");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setNickname("NewUser");
        registerRequest.setGender("male");
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            when(userRepository.findByEmailOrUid("test@example.com", "test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            // AuthService now uses findByDeviceId first (globally) before findByUserIdAndDeviceId
            when(userDeviceRepository.findByDeviceId("device-123"))
                    .thenReturn(Optional.empty());
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(1L, "device-123")).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L, "device-123")).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

            // When
            LoginResponse response = authService.login(loginRequest, "192.168.1.1");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid email")
        void shouldThrowExceptionForInvalidEmail() {
            // Given
            when(userRepository.findByEmailOrUid("test@example.com", "test@example.com"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(loginRequest, "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email/UID or password");
        }

        @Test
        @DisplayName("Should throw exception for invalid password")
        void shouldThrowExceptionForInvalidPassword() {
            // Given
            when(userRepository.findByEmailOrUid("test@example.com", "test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.login(loginRequest, "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email/UID or password");
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            // Given
            testUser.setStatus(User.UserStatus.inactive);
            when(userRepository.findByEmailOrUid("test@example.com", "test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.login(loginRequest, "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Account is inactive");
        }

        @Test
        @DisplayName("Should throw exception for banned user")
        void shouldThrowExceptionForBannedUser() {
            // Given
            testUser.setStatus(User.UserStatus.banned);
            when(userRepository.findByEmailOrUid("test@example.com", "test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.login(loginRequest, "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Account is banned");
        }

        @Test
        @DisplayName("Should update existing device on login")
        void shouldUpdateExistingDeviceOnLogin() {
            // Given
            UserDevice existingDevice = UserDevice.builder()
                    .id(1L)
                    .user(testUser)
                    .deviceId("device-123")
                    .deviceType(UserDevice.DeviceType.web)
                    .isOnline(false)
                    .build();

            when(userRepository.findByEmailOrUid("test@example.com", "test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            // AuthService now uses findByDeviceId first (globally)
            when(userDeviceRepository.findByDeviceId("device-123"))
                    .thenReturn(Optional.of(existingDevice));
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(1L, "device-123")).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L, "device-123")).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

            // When
            LoginResponse response = authService.login(loginRequest, "192.168.1.1");

            // Then
            assertThat(response).isNotNull();

            ArgumentCaptor<UserDevice> deviceCaptor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(deviceCaptor.capture());

            UserDevice savedDevice = deviceCaptor.getValue();
            assertThat(savedDevice.getIsOnline()).isTrue();
            assertThat(savedDevice.getDeviceName()).isEqualTo("Chrome Browser");
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterSuccessfully() {
            // Given
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByUid(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User user = inv.getArgument(0);
                user.setId(2L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // When
            UserResponse response = authService.register(registerRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("newuser@example.com");
            assertThat(response.getNickname()).isEqualTo("NewUser");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
            assertThat(savedUser.getPasswordHash()).isEqualTo("hashedPassword");
            assertThat(savedUser.getGender()).isEqualTo(User.Gender.male);
            assertThat(savedUser.getStatus()).isEqualTo(User.UserStatus.active);
            assertThat(savedUser.getUid()).startsWith("LC");
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionForDuplicateEmail() {
            // Given
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email already exists");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should generate unique UID")
        void shouldGenerateUniqueUid() {
            // Given
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByUid(anyString()))
                    .thenReturn(true)  // First attempt - UID exists
                    .thenReturn(false); // Second attempt - UID is unique
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User user = inv.getArgument(0);
                user.setId(2L);
                return user;
            });

            // When
            authService.register(registerRequest);

            // Then
            verify(userRepository, times(2)).existsByUid(anyString());
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            String refreshToken = "valid-refresh-token";
            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(1L);
            when(jwtTokenProvider.getDeviceIdFromToken(refreshToken)).thenReturn("device-123");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userDeviceRepository.findByUserIdAndDeviceId(1L, "device-123"))
                    .thenReturn(Optional.of(UserDevice.builder()
                            .id(1L)
                            .user(testUser)
                            .deviceId("device-123")
                            .deviceType(UserDevice.DeviceType.web)
                            .build()));
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(1L, "device-123")).thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken(1L, "device-123")).thenReturn("new-refresh-token");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

            // When
            LoginResponse response = authService.refreshToken(refreshToken, "192.168.1.1");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken("invalid-token", "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(999L);
            when(jwtTokenProvider.getDeviceIdFromToken("valid-token")).thenReturn("device-123");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken("valid-token", "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should throw exception when user is inactive")
        void shouldThrowExceptionWhenUserInactive() {
            // Given
            testUser.setStatus(User.UserStatus.inactive);
            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(1L);
            when(jwtTokenProvider.getDeviceIdFromToken("valid-token")).thenReturn("device-123");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken("valid-token", "192.168.1.1"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Account is inactive");
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() {
            // Given
            UserDevice device = UserDevice.builder()
                    .id(1L)
                    .user(testUser)
                    .deviceId("device-123")
                    .deviceType(UserDevice.DeviceType.web)
                    .isOnline(true)
                    .pushToken("push-token")
                    .build();

            when(userDeviceRepository.findByUserIdAndDeviceId(1L, "device-123"))
                    .thenReturn(Optional.of(device));
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.logout(1L, "device-123");

            // Then
            ArgumentCaptor<UserDevice> deviceCaptor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(deviceCaptor.capture());

            UserDevice savedDevice = deviceCaptor.getValue();
            assertThat(savedDevice.getIsOnline()).isFalse();
            assertThat(savedDevice.getPushToken()).isNull();
        }

        @Test
        @DisplayName("Should handle logout when device not found")
        void shouldHandleLogoutWhenDeviceNotFound() {
            // Given
            when(userDeviceRepository.findByUserIdAndDeviceId(1L, "device-123"))
                    .thenReturn(Optional.empty());

            // When
            authService.logout(1L, "device-123");

            // Then
            verify(userDeviceRepository, never()).save(any(UserDevice.class));
        }
    }
}
