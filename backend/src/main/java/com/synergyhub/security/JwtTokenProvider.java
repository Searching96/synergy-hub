package com.synergyhub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    // ✅ Validate key strength on startup
    @PostConstruct
    private void validateSecretKey() {
        // CRITICAL: JWT secret must be set via environment variable
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable must be set. " +
                "Application cannot start without a valid JWT secret."
            );
        }
        
        // HS512 requires at least 64 bytes (512 bits)
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException(
                "JWT secret must be at least 64 bytes (512 bits) for HS512 algorithm. " +
                "Current length: " + jwtSecret.getBytes(StandardCharsets.UTF_8).length + ". " +
                "Use a cryptographically secure random string."
            );
        }
        log.info("JWT secret key validated successfully (length: {} bytes)", 
                  jwtSecret.getBytes(StandardCharsets.UTF_8).length);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Use the JCA name for HS512 directly
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateTokenFromUserId(userPrincipal.getId(), userPrincipal.getEmail());
    }

    public String generateTokenFromUserId(Integer userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512) // ✅ Explicit algorithm parameter
                .compact();
    }

    public String generateTemporaryToken(Integer userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 300000); // 5 minutes

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("temp", true)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512) // ✅ Explicit algorithm parameter
                .compact();
    }

    public Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Integer.parseInt(claims.getSubject());
    }

    public String getTokenIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("jti", String.class);
    }

    public boolean isTemporaryToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("temp", Boolean.class) != null && claims.get("temp", Boolean.class);
    }

    /**
     * Extracts the JWT token from the Authorization header of the request.
     */
    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String authToken) {
        try {
            // 1. Parse the token
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);

            // 2. ✅ Explicitly validate the algorithm in the header
            // This ensures strictly HS512 is used, preventing any subtle confusion attacks
            String tokenAlgorithm = jws.getHeader().getAlgorithm();
            if (!Jwts.SIG.HS512.getId().equals(tokenAlgorithm)) {
                log.error("Invalid JWT algorithm: expected {}, found {}", Jwts.SIG.HS512.getId(), tokenAlgorithm);
                return false;
            }

            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}