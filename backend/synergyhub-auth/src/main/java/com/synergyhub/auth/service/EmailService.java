package com.synergyhub.auth.service;

import com.synergyhub.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(User user, String token) {
        try {
            String verificationLink = frontendUrl + "/verify-email?token=" + token;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("SynergyHub - Verify Your Email");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Thank you for registering with SynergyHub!\n\n" +
                "Please verify your email address by clicking the link below:\n%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not create this account, please ignore this email.\n\n" +
                "Best regards,\nThe SynergyHub Team",
                user.getName(),
                verificationLink
            ));

            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("SynergyHub - Password Reset Request");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "We received a request to reset your password.\n\n" +
                "Click the link below to reset your password:\n%s\n\n" +
                "This link will expire in 15 minutes.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\nThe SynergyHub Team",
                user.getName(),
                resetLink
            ));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendAccountLockedEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("SynergyHub - Account Locked");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Your account has been temporarily locked due to multiple failed login attempts.\n\n" +
                "Your account will be automatically unlocked in 30 minutes.\n\n" +
                "If you believe this was an error or you need immediate assistance, please contact support.\n\n" +
                "Best regards,\nThe SynergyHub Team",
                user.getName()
            ));

            mailSender.send(message);
            log.info("Account locked email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send account locked email to: {}", user.getEmail(), e);
        }
    }
}