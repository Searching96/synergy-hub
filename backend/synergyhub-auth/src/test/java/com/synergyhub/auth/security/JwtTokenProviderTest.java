package com.synergyhub.auth.security;

import com.synergyhub.auth.entity.Permission;
import com.synergyhub.auth.entity.Role;
import com.synergyhub.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String jwtSecret = "mySecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmPleaseChangeThis";
    private final long jwtExpirationMs = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(jwtSecret, jwtExpirationMs);
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // Arrange
        User user = createTestUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // Act
        String token = tokenProvider.generateToken(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void generateToken_ShouldContainCorrectClaims() {
        // Arrange
        User user = createTestUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // Act
        String token = tokenProvider.generateToken(authentication);

        // Assert
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("test@example.com", claims.getSubject());
        assertEquals(1, claims.get("userId"));
        assertEquals(1, claims.get("organizationId"));
        assertNotNull(claims.getId()); // JWT ID (jti)
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void getEmailFromToken_ShouldReturnCorrectEmail() {
        // Arrange
        User user = createTestUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = tokenProvider.generateToken(authentication);

        // Act
        String email = tokenProvider.getEmailFromToken(token);

        // Assert
        assertEquals("test@example.com", email);
    }

    @Test
    void getTokenId_ShouldReturnUniqueId() {
        // Arrange
        User user = createTestUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = tokenProvider.generateToken(authentication);

        // Act
        String tokenId = tokenProvider.getTokenId(token);

        // Assert
        assertNotNull(tokenId);
        assertTrue(tokenId.length() > 0);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Arrange
        User user = createTestUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = tokenProvider.generateToken(authentication);

        // Act
        boolean isValid = tokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = tokenProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithTamperedToken_ShouldReturnFalse() {
        // Arrange
        User user = createTestUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = tokenProvider.generateToken(authentication);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // Act
        boolean isValid = tokenProvider.validateToken(tamperedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void getExpirationMs_ShouldReturnCorrectValue() {
        // Act
        long expiration = tokenProvider.getExpirationMs();

        // Assert
        assertEquals(jwtExpirationMs, expiration);
    }

    private User createTestUser() {
        Permission permission = Permission.builder()
                .permId(1)
                .name("VIEW_PROJECT")
                .build();

        Role role = Role.builder()
                .roleId(1)
                .name("Team Member")
                .permissions(Set.of(permission))
                .build();

        return User.builder()
                .userId(1)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .organizationId(1)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(Set.of(role))
                .build();
    }
}