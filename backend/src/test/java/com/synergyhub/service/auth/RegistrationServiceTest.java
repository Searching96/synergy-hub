package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.EmailVerification;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.RegisterRequest;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.exception.BadRequestException;
import com.synergyhub.exception.EmailAlreadyExistsException;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.EmailVerificationRepository;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.RoleRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.security.AuditLogService;
import com.synergyhub.util.EmailService;
import com.synergyhub.util.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RegistrationService registrationService;

    private RegisterRequest registerRequest;
    private Organization testOrganization;
    private Role defaultRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationService, "emailVerificationEnabled", true);
        ReflectionTestUtils.setField(registrationService, "defaultOrganizationId", 1);

        registerRequest = RegisterRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("SecurePass123")
                .build();

        testOrganization = Organization.builder()
                .id(1)
                .name("Default Organization")
                .build();

        defaultRole = Role.builder()
                .id(3)
                .name("Team Member")
                .description("Regular team member")
                .build();
    }

    @Test
    void register_WithValidData_ShouldCreateUserAndSendVerificationEmail() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordValidator.isValid(registerRequest.getPassword())).thenReturn(true);
        when(organizationRepository.findById(1)).thenReturn(Optional.of(testOrganization));
        when(roleRepository.findByName("Team Member")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");

        User savedUser = User.builder()
                .id(1)
                .email(registerRequest.getEmail())
                .name(registerRequest.getName())
                .organization(testOrganization)
                .emailVerified(false)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(
                UserResponse.builder()
                        .id(1)
                        .email(registerRequest.getEmail())
                        .name(registerRequest.getName())
                        .build()
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);

        // Capture the current time BEFORE the test
        LocalDateTime beforeTest = LocalDateTime.now();

        // When
        UserResponse response = registrationService.register(registerRequest, "127.0.0.1");

        // Capture the time AFTER the test
        LocalDateTime afterTest = LocalDateTime.now();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(response.getName()).isEqualTo(registerRequest.getName());

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(capturedUser.getName()).isEqualTo(registerRequest.getName());
        assertThat(capturedUser.getOrganization()).isEqualTo(testOrganization);
        assertThat(capturedUser.getEmailVerified()).isFalse();
        assertThat(capturedUser.getRoles()).contains(defaultRole);

        verify(emailVerificationRepository).save(verificationCaptor.capture());
        EmailVerification verification = verificationCaptor.getValue();
        assertThat(verification.getUser()).isEqualTo(savedUser);
        assertThat(verification.getToken()).isNotNull();

        // Change the assertion to check it's between beforeTest and afterTest
        assertThat(verification.getExpiryTime())
                .isAfterOrEqualTo(beforeTest)
                .isBeforeOrEqualTo(afterTest);

        verify(emailService).sendEmailVerification(eq(registerRequest.getEmail()), anyString());
        verify(auditLogService).logUserCreated(any(User.class), eq("127.0.0.1"));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowEmailAlreadyExistsException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registrationService.register(registerRequest, "127.0.0.1"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(registerRequest.getEmail());

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerification(anyString(), anyString());
    }

    @Test
    void register_WithWeakPassword_ShouldThrowBadRequestException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordValidator.isValid(registerRequest.getPassword())).thenReturn(false);
        when(passwordValidator.getRequirements()).thenReturn("Password must be at least 8 characters");

        // When & Then
        assertThatThrownBy(() -> registrationService.register(registerRequest, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password does not meet requirements");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WithSpecificOrganization_ShouldAssignToThatOrganization() {
        // Given
        Organization specificOrg = Organization.builder()
                .id(2)
                .name("Specific Organization")
                .build();

        registerRequest.setOrganizationId(2);

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordValidator.isValid(registerRequest.getPassword())).thenReturn(true);
        when(organizationRepository.findById(2)).thenReturn(Optional.of(specificOrg));
        when(roleRepository.findByName("Team Member")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");

        User savedUser = User.builder()
                .id(1)
                .email(registerRequest.getEmail())
                .name(registerRequest.getName())
                .organization(specificOrg)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(UserResponse.builder().build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // When
        registrationService.register(registerRequest, "127.0.0.1");

        // Then
        verify(userRepository).save(captor.capture());
        User capturedUser = captor.getValue();
        assertThat(capturedUser.getOrganization()).isEqualTo(specificOrg);
    }

    @Test
    void register_WithNonExistentOrganization_ShouldThrowResourceNotFoundException() {
        // Given
        registerRequest.setOrganizationId(999);

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordValidator.isValid(registerRequest.getPassword())).thenReturn(true);
        when(organizationRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.register(registerRequest, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WithEmailVerificationDisabled_ShouldCreateVerifiedUser() {
        // Given
        ReflectionTestUtils.setField(registrationService, "emailVerificationEnabled", false);

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordValidator.isValid(registerRequest.getPassword())).thenReturn(true);
        when(organizationRepository.findById(1)).thenReturn(Optional.of(testOrganization));
        when(roleRepository.findByName("Team Member")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");

        User savedUser = User.builder()
                .id(1)
                .email(registerRequest.getEmail())
                .name(registerRequest.getName())
                .organization(testOrganization)
                .emailVerified(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(UserResponse.builder().build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // When
        registrationService.register(registerRequest, "127.0.0.1");

        // Then
        verify(userRepository).save(captor.capture());
        User capturedUser = captor.getValue();
        assertThat(capturedUser.getEmailVerified()).isTrue();

        verify(emailVerificationRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerification(anyString(), anyString());
        verify(emailService).sendWelcomeEmail(registerRequest.getEmail(), registerRequest.getName());
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyEmail() {
        // Given
        String token = "valid-token-123";
        User user = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .organization(testOrganization)
                .emailVerified(false)
                .build();

        EmailVerification verification = EmailVerification.builder()
                .id(1)
                .user(user)
                .token(token)
                .verified(false)
                .expiryTime(LocalDateTime.now().plusHours(1))
                .build();

        when(emailVerificationRepository.findByToken(token)).thenReturn(Optional.of(verification));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);

        // When
        registrationService.verifyEmail(token, "127.0.0.1");

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmailVerified()).isTrue();

        verify(emailVerificationRepository).save(verificationCaptor.capture());
        EmailVerification savedVerification = verificationCaptor.getValue();
        assertThat(savedVerification.getVerified()).isTrue();

        verify(emailService).sendWelcomeEmail(user.getEmail(), user.getName());
        verify(auditLogService).logEmailVerified(user, "127.0.0.1");
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldThrowBadRequestException() {
        // Given
        String token = "invalid-token";

        when(emailVerificationRepository.findByToken(token)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.verifyEmail(token, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired verification token");

        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowBadRequestException() {
        // Given
        String token = "expired-token";
        User user = User.builder()
                .id(1)
                .email("test@example.com")
                .emailVerified(false)
                .build();

        EmailVerification verification = EmailVerification.builder()
                .id(1)
                .user(user)
                .token(token)
                .verified(false)
                .expiryTime(LocalDateTime.now().minusHours(1)) // Expired
                .build();

        when(emailVerificationRepository.findByToken(token)).thenReturn(Optional.of(verification));

        // When & Then
        assertThatThrownBy(() -> registrationService.verifyEmail(token, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_WithAlreadyVerifiedToken_ShouldThrowBadRequestException() {
        // Given
        String token = "already-verified-token";
        User user = User.builder()
                .id(1)
                .email("test@example.com")
                .emailVerified(true)
                .build();

        EmailVerification verification = EmailVerification.builder()
                .id(1)
                .user(user)
                .token(token)
                .verified(true)
                .expiryTime(LocalDateTime.now().plusHours(1))
                .build();

        when(emailVerificationRepository.findByToken(token)).thenReturn(Optional.of(verification));

        // When & Then
        assertThatThrownBy(() -> registrationService.verifyEmail(token, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been verified");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationEmail_WithValidEmail_ShouldSendNewVerificationEmail() {
        // Given
        String email = "test@example.com";
        User user = User.builder()
                .id(1)
                .email(email)
                .name("Test User")
                .organization(testOrganization)
                .emailVerified(false)
                .build();

        EmailVerification oldVerification = EmailVerification.builder()
                .id(1)
                .user(user)
                .token("old-token")
                .verified(false)
                .expiryTime(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findByUserAndVerifiedFalse(user))
                .thenReturn(Optional.of(oldVerification));

        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);

        // Capture time before test
        LocalDateTime beforeTest = LocalDateTime.now();

        // When
        registrationService.resendVerificationEmail(email);

        // Capture time after test
        LocalDateTime afterTest = LocalDateTime.now();

        // Then
        verify(emailVerificationRepository).delete(oldVerification);
        verify(emailVerificationRepository).save(captor.capture());

        EmailVerification newVerification = captor.getValue();
        assertThat(newVerification.getUser()).isEqualTo(user);
        assertThat(newVerification.getToken()).isNotNull();
        assertThat(newVerification.getToken()).isNotEqualTo("old-token");

        // Change the assertion to check it's between beforeTest and afterTest
        assertThat(newVerification.getExpiryTime())
                .isAfterOrEqualTo(beforeTest)
                .isBeforeOrEqualTo(afterTest);

        verify(emailService).sendEmailVerification(eq(email), anyString());
    }

    @Test
    void resendVerificationEmail_WithAlreadyVerifiedEmail_ShouldThrowBadRequestException() {
        // Given
        String email = "test@example.com";
        User user = User.builder()
                .id(1)
                .email(email)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> registrationService.resendVerificationEmail(email))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already verified");

        verify(emailVerificationRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerification(anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_WithNonExistentEmail_ShouldThrowResourceNotFoundException() {
        // Given
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.resendVerificationEmail(email))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(emailVerificationRepository, never()).save(any());
    }
}