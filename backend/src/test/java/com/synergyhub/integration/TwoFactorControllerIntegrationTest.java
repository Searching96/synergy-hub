package com.synergyhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.config.TestSecurityConfig;
import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.LoginRequest;
import com.synergyhub.dto.request.TwoFactorDisableRequest;
import com.synergyhub.dto.request.TwoFactorVerifyRequest;
import com.synergyhub.dto.response.LoginResponse;
import com.synergyhub.dto.response.TwoFactorSetupResponse;
import com.synergyhub.repository.*;
import com.synergyhub.security.JwtTokenProvider;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TwoFactorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TwoFactorSecretRepository twoFactorSecretRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private String accessToken;
    private CodeGenerator codeGenerator;
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        // Clean up
        userSessionRepository.deleteAll();
        twoFactorSecretRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();

        // Setup test data
        Organization org = Organization.builder()
                .name("Test Organization")
                .address("123 Test Street")
                .build();
        org = organizationRepository.save(org);

        Role role = Role.builder()
                .name("Team Member")
                .description("Regular team member")
                .build();
        roleRepository.save(role);

        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .organization(org)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
        testUser.getRoles().add(role);
        testUser = userRepository.save(testUser);

        // Generate access token
        accessToken = jwtTokenProvider.generateTokenFromUserId(testUser.getId(), testUser.getEmail());

        // Initialize TOTP code generator
        codeGenerator = new DefaultCodeGenerator();
        timeProvider = new SystemTimeProvider();
    }

    @Test
    void complete2FAFlow_ViaAPI_ShouldWorkCorrectly() throws Exception {
        // Step 1: Setup 2FA
        MvcResult setupResult = mockMvc.perform(post("/api/auth/2fa/setup")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secret").exists())
                .andExpect(jsonPath("$.data.qrCodeUrl").exists())
                .andExpect(jsonPath("$.data.backupCodes").isArray())
                .andReturn();

        String setupJson = setupResult.getResponse().getContentAsString();
        TwoFactorSetupResponse setupResponse = objectMapper.readValue(
                objectMapper.readTree(setupJson).get("data").toString(),
                TwoFactorSetupResponse.class
        );

        // Step 2: Generate valid TOTP code
        String validCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );

        // Step 3: Verify and enable 2FA
        TwoFactorVerifyRequest verifyRequest = TwoFactorVerifyRequest.builder()
                .code(validCode)
                .build();

        mockMvc.perform(post("/api/auth/2fa/verify")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Two-factor authentication enabled successfully"));

        // Verify in database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getTwoFactorEnabled()).isTrue();

        // Step 4: First login attempt - should return twoFactorRequired
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.data.twoFactorToken").exists())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist()) // No access token yet
                .andReturn();

        String loginJson = loginResult.getResponse().getContentAsString();
        String twoFactorToken = objectMapper.readTree(loginJson)
                .get("data")
                .get("twoFactorToken")
                .asText();

        assertThat(twoFactorToken).isNotNull();

        // Step 5: Generate a fresh TOTP code for second login attempt
        // Wait a moment to ensure we're not reusing the same code
        Thread.sleep(1000);

        String loginCode = codeGenerator.generate(
                setupResponse.getSecret(),
                timeProvider.getTime() / 30
        );

        // Step 6: Second login attempt WITH 2FA code
        LoginRequest loginWith2FA = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .twoFactorCode(loginCode) // NOW include the 2FA code
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWith2FA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.twoFactorRequired").value(false))
                .andExpect(jsonPath("$.data.accessToken").exists()) // Now we get access token
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user").exists());

        // Step 7: Disable 2FA
        TwoFactorDisableRequest disableRequest = TwoFactorDisableRequest.builder()
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/2fa/disable")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify 2FA is disabled
        User finalUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(finalUser.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void verify2FA_WithInvalidCode_ShouldReturnBadRequest() throws Exception {
        // Setup 2FA first
        mockMvc.perform(post("/api/auth/2fa/setup")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Try to verify with invalid code
        TwoFactorVerifyRequest verifyRequest = TwoFactorVerifyRequest.builder()
                .code("000000")
                .build();

        mockMvc.perform(post("/api/auth/2fa/verify")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid verification code"));
    }

    @Test
    void disable2FA_WithWrongPassword_ShouldReturnBadRequest() throws Exception {
        // Enable 2FA for user
        testUser.setTwoFactorEnabled(true);
        userRepository.save(testUser);

        TwoFactorDisableRequest disableRequest = TwoFactorDisableRequest.builder()
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/2fa/disable")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}