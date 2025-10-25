package com.synergyhub.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final JwtParser jwtParser;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:mySecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmPleaseChangeThis}") String jwtSecret,
            @Value("${app.jwt.expiration:86400000}") long jwtExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtParser = Jwts.parser()
                .verifyWith(key)
                .build();
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userDetails.getEmail())
                .claim("userId", userDetails.getUserId())
                .claim("organizationId", userDetails.getOrganizationId())
                .claim("authorities", authorities)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = jwtParser
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public String getTokenId(String token) {
        Claims claims = jwtParser
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}