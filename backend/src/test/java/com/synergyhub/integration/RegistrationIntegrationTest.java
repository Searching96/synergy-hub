package com.synergyhub.integration;

import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.RegisterRequest;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.repository.EmailVerificationRepository;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.RoleRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.service.auth.RegistrationService;
import com.synergyhub.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class RegistrationIntegrationTest {

    @MockBean
    private EmailService emailService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Mock email service to do nothing
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), any(User.class), anyString());
        doNothing().when(emailService).sendEmailVerification(anyString(), anyString(), any(User.class), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), any(User.class), anyString());

        // Clean up
        emailVerificationRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();

        // Setup test data
        testOrganization = Organization.builder()
                .name("Test Organization")
                .address("123 Test Street")
                .build();
        testOrganization = organizationRepository.save(testOrganization);

        Role testRole = Role.builder()
                .name("Team Member")
                .description("Regular team member")
                .build();
        testRole = roleRepository.save(testRole);
    }

    @Test
    void register_EndToEnd_ShouldCreateUserSuccessfully() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("SecurePass123")
                .organizationId(testOrganization.getId())
                .build();

        // When
        UserResponse response = registrationService.register(request, "127.0.0.1");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getName()).isEqualTo(request.getName());

        // Verify user in database
        User savedUser = userRepository.findByEmail(request.getEmail()).orElseThrow();
        assertThat(savedUser.getName()).isEqualTo(request.getName());
        assertThat(savedUser.getOrganization().getId()).isEqualTo(testOrganization.getId());
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(savedUser.getRoles()).extracting(Role::getName).contains("Team Member");
    }
}