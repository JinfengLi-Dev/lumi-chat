package com.lumichat.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@lumichat.com}")
    private String fromAddress;

    @Async
    public void sendPasswordResetEmail(String to, String resetUrl) {
        try {
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Reset Your Lumi Chat Password");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Password reset email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String nickname) {
        try {
            Context context = new Context();
            context.setVariable("nickname", nickname);

            String htmlContent = templateEngine.process("welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Welcome to Lumi Chat!");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
            // Don't throw - welcome email is not critical
        }
    }
}
