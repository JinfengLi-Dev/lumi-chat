package com.lumichat.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@lumichat.com");
    }

    @Test
    @DisplayName("Should send password reset email with correct template")
    void shouldSendPasswordResetEmail() throws Exception {
        // Given
        String to = "user@example.com";
        String resetUrl = "https://lumichat.com/reset-password?token=abc123";
        String expectedHtml = "<html>Password reset email</html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("password-reset"), any(Context.class))).thenReturn(expectedHtml);

        // When
        emailService.sendPasswordResetEmail(to, resetUrl);

        // Then
        verify(templateEngine).process(eq("password-reset"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should include reset URL in template context")
    void shouldIncludeResetUrlInContext() throws Exception {
        // Given
        String to = "user@example.com";
        String resetUrl = "https://lumichat.com/reset-password?token=abc123";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("password-reset"), contextCaptor.capture())).thenReturn("<html></html>");

        // When
        emailService.sendPasswordResetEmail(to, resetUrl);

        // Then
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("resetUrl")).isEqualTo(resetUrl);
    }

    @Test
    @DisplayName("Should send welcome email with correct template")
    void shouldSendWelcomeEmail() throws Exception {
        // Given
        String to = "newuser@example.com";
        String nickname = "New User";
        String expectedHtml = "<html>Welcome email</html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome"), any(Context.class))).thenReturn(expectedHtml);

        // When
        emailService.sendWelcomeEmail(to, nickname);

        // Then
        verify(templateEngine).process(eq("welcome"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should include nickname in welcome email context")
    void shouldIncludeNicknameInWelcomeContext() throws Exception {
        // Given
        String to = "newuser@example.com";
        String nickname = "Test User";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("welcome"), contextCaptor.capture())).thenReturn("<html></html>");

        // When
        emailService.sendWelcomeEmail(to, nickname);

        // Then
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("nickname")).isEqualTo(nickname);
    }

    @Test
    @DisplayName("Should throw exception when password reset email fails")
    void shouldThrowExceptionWhenPasswordResetFails() throws Exception {
        // Given
        String to = "user@example.com";
        String resetUrl = "https://lumichat.com/reset-password?token=abc123";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("password-reset"), any(Context.class))).thenReturn("<html></html>");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        // When / Then
        assertThatThrownBy(() -> emailService.sendPasswordResetEmail(to, resetUrl))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should not throw exception when welcome email fails")
    void shouldNotThrowExceptionWhenWelcomeEmailFails() throws Exception {
        // Given
        String to = "newuser@example.com";
        String nickname = "New User";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome"), any(Context.class))).thenReturn("<html></html>");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        // When / Then - should not throw
        emailService.sendWelcomeEmail(to, nickname);

        // Verify mail was attempted
        verify(mailSender).send(mimeMessage);
    }
}
