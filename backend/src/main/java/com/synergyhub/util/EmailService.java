package com.synergyhub.util;

import com.synergyhub.domain.entity.User;
import com.synergyhub.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;  // ✅ Added

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token, User user, String ipAddress) {  // ✅ Added user and ipAddress
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - SynergyHub");
        message.setText("Hello,\n\n" +
                "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                resetUrl + "\n\n" +
                "This link will expire in 15 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "SynergyHub Team");

        try {
            mailSender.send(message);

            // ✅ Audit log for successful email send
            auditLogService.createAuditLog(
                    user,
                    "PASSWORD_RESET_EMAIL_SENT",
                    String.format("Password reset email sent to: %s", toEmail),
                    ipAddress
            );
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            // ✅ Audit log for failed email send
            auditLogService.createAuditLog(
                    user,
                    "PASSWORD_RESET_EMAIL_FAILED",
                    String.format("Failed to send password reset email to: %s. Error: %s", toEmail, e.getMessage()),
                    ipAddress
            );
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendEmailVerification(String toEmail, String token, User user, String ipAddress) {  // ✅ Added user and ipAddress
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Email Verification - SynergyHub");
        message.setText("Hello,\n\n" +
                "Welcome to SynergyHub! Please verify your email address by clicking the link below:\n\n" +
                verificationUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Best regards,\n" +
                "SynergyHub Team");

        try {
            mailSender.send(message);

            // ✅ Audit log for successful email send
            auditLogService.createAuditLog(
                    user,
                    "EMAIL_VERIFICATION_SENT",
                    String.format("Email verification sent to: %s", toEmail),
                    ipAddress
            );
            log.info("Email verification sent to: {}", toEmail);
        } catch (Exception e) {
            // ✅ Audit log for failed email send
            auditLogService.createAuditLog(
                    user,
                    "EMAIL_VERIFICATION_FAILED",
                    String.format("Failed to send verification email to: %s. Error: %s", toEmail, e.getMessage()),
                    ipAddress
            );
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String name, User user, String ipAddress) {  // ✅ Added user and ipAddress
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to SynergyHub!");
        message.setText("Hello " + name + ",\n\n" +
                "Welcome to SynergyHub! Your account has been successfully created.\n\n" +
                "You can now log in and start collaborating with your team.\n\n" +
                "Best regards,\n" +
                "SynergyHub Team");

        try {
            mailSender.send(message);

            // ✅ Audit log for successful email send
            auditLogService.createAuditLog(
                    user,
                    "WELCOME_EMAIL_SENT",
                    String.format("Welcome email sent to: %s (Name: %s)", toEmail, name),
                    ipAddress
            );
            log.info("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            // ✅ Audit log for failed email send (non-critical, so don't throw)
            auditLogService.createAuditLog(
                    user,
                    "WELCOME_EMAIL_FAILED",
                    String.format("Failed to send welcome email to: %s. Error: %s", toEmail, e.getMessage()),
                    ipAddress
            );
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email failure - it's not critical
        }
    }

    // ✅ Optional: Add method to send project invitation email
    public void sendProjectInvitationEmail(String toEmail, String projectName, String inviterName,
                                           User inviter, String ipAddress) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Project Invitation - SynergyHub");
        message.setText("Hello,\n\n" +
                inviterName + " has invited you to join the project '" + projectName + "' on SynergyHub.\n\n" +
                "Log in to your account to view the project:\n" +
                frontendUrl + "/projects\n\n" +
                "Best regards,\n" +
                "SynergyHub Team");

        try {
            mailSender.send(message);

            auditLogService.createAuditLog(
                    inviter,
                    "PROJECT_INVITATION_EMAIL_SENT",
                    String.format("Project invitation email sent to: %s for project: %s", toEmail, projectName),
                    ipAddress
            );
            log.info("Project invitation email sent to: {} for project: {}", toEmail, projectName);
        } catch (Exception e) {
            auditLogService.createAuditLog(
                    inviter,
                    "PROJECT_INVITATION_EMAIL_FAILED",
                    String.format("Failed to send project invitation to: %s. Error: %s", toEmail, e.getMessage()),
                    ipAddress
            );
            log.error("Failed to send project invitation email to: {}", toEmail, e);
        }
    }

    // ✅ Optional: Add method to send account locked notification
    public void sendAccountLockedEmail(String toEmail, User user, String ipAddress) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Account Security Alert - SynergyHub");
        message.setText("Hello,\n\n" +
                "Your account has been temporarily locked due to multiple failed login attempts.\n\n" +
                "If this was you, please contact your administrator or wait 30 minutes before trying again.\n\n" +
                "If this was not you, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "SynergyHub Team");

        try {
            mailSender.send(message);

            auditLogService.createAuditLog(
                    user,
                    "ACCOUNT_LOCKED_EMAIL_SENT",
                    String.format("Account locked notification sent to: %s", toEmail),
                    ipAddress
            );
            log.info("Account locked notification sent to: {}", toEmail);
        } catch (Exception e) {
            auditLogService.createAuditLog(
                    user,
                    "ACCOUNT_LOCKED_EMAIL_FAILED",
                    String.format("Failed to send account locked notification to: %s. Error: %s", toEmail, e.getMessage()),
                    ipAddress
            );
            log.error("Failed to send account locked notification to: {}", toEmail, e);
        }
    }

    // ✅ Optional: Add method to send password changed notification
    public void sendPasswordChangedEmail(String toEmail, User user, String ipAddress) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Changed - SynergyHub");
        message.setText("Hello,\n\n" +
                "Your password has been successfully changed.\n\n" +
                "If you did not make this change, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "SynergyHub Team");

        try {
            mailSender.send(message);

            auditLogService.createAuditLog(
                    user,
                    "PASSWORD_CHANGED_EMAIL_SENT",
                    String.format("Password changed notification sent to: %s", toEmail),
                    ipAddress
            );
            log.info("Password changed notification sent to: {}", toEmail);
        } catch (Exception e) {
            auditLogService.createAuditLog(
                    user,
                    "PASSWORD_CHANGED_EMAIL_FAILED",
                    String.format("Failed to send password changed notification to: %s. Error: %s", toEmail, e.getMessage()),
                    ipAddress
            );
            log.error("Failed to send password changed notification to: {}", toEmail, e);
        }
    }
}
