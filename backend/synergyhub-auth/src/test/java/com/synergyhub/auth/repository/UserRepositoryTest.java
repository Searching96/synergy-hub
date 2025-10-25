package com.synergyhub.auth.repository;

import com.synergyhub.auth.AllTestsSuite;
import com.synergyhub.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = AllTestsSuite.class)
@ActiveProfiles("test")
@Disabled("Requires full Spring context - enable for integration testing")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .organizationId(1)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void findByEmail_WhenExists_ShouldReturnUser() {
        // Arrange
        entityManager.persist(testUser);
        entityManager.flush();

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void existsByEmail_WhenExists_ShouldReturnTrue() {
        // Arrange
        entityManager.persist(testUser);
        entityManager.flush();

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByEmail_WhenNotExists_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    void findActiveUserByEmail_WhenLockedAccount_ShouldReturnEmpty() {
        // Arrange
        testUser.setAccountLocked(true);
        entityManager.persist(testUser);
        entityManager.flush();

        // Act
        Optional<User> found = userRepository.findActiveUserByEmail("test@example.com");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findUsersWithExpiredLocks_ShouldReturnExpiredLockedUsers() {
        // Arrange
        testUser.setAccountLocked(true);
        testUser.setLockUntil(LocalDateTime.now().minusHours(1));
        entityManager.persist(testUser);
        entityManager.flush();

        // Act
        List<User> users = userRepository.findUsersWithExpiredLocks(LocalDateTime.now());

        // Assert
        assertEquals(1, users.size());
        assertEquals("test@example.com", users.get(0).getEmail());
    }

    @Test
    void findByOrganizationId_ShouldReturnUsersInOrganization() {
        // Arrange
        entityManager.persist(testUser);

        User anotherUser = User.builder()
                .name("Another User")
                .email("another@example.com")
                .passwordHash("hash")
                .organizationId(1)
                .roles(new HashSet<>())
                .build();
        entityManager.persist(anotherUser);
        entityManager.flush();

        // Act
        List<User> users = userRepository.findByOrganizationId(1);

        // Assert
        assertEquals(2, users.size());
    }
}