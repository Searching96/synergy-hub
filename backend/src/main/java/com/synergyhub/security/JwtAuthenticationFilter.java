package com.synergyhub.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.synergyhub.service.auth.SessionService;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final SessionService sessionService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Integer userId = tokenProvider.getUserIdFromToken(jwt);
                String tokenId = tokenProvider.getTokenIdFromToken(jwt);
                
                // ✅ CHECK IF SESSION IS REVOKED
                if (sessionService.isSessionRevoked(tokenId)) {
                    log.warn("Attempted to use revoked token: {} from IP: {}", tokenId, request.getRemoteAddr());
                    response.setHeader("X-Auth-Error", "SESSION_REVOKED");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT token from IP: {}", request.getRemoteAddr());
            response.setHeader("X-Auth-Error", "TOKEN_EXPIRED");
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token from IP: {} - User-Agent: {}", 
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
            response.setHeader("X-Auth-Error", "TOKEN_MALFORMED");
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature from IP: {} - Possible tampering attempt", 
                    request.getRemoteAddr());
            response.setHeader("X-Auth-Error", "INVALID_SIGNATURE");
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token from IP: {}", request.getRemoteAddr());
            response.setHeader("X-Auth-Error", "TOKEN_UNSUPPORTED");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty from IP: {}", request.getRemoteAddr());
            response.setHeader("X-Auth-Error", "TOKEN_INVALID");
        } catch (Exception ex) {
            log.error("Unexpected error during JWT authentication from IP: {} - Error: {}", 
                    request.getRemoteAddr(), ex.getMessage(), ex);
            response.setHeader("X-Auth-Error", "INTERNAL_ERROR");
        }
        
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}