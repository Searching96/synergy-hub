package com.synergyhub.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ClientIpResolver {

    // ✅ IP validation pattern
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
        "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // ✅ IPv6 pattern (simplified)
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    @Value("${app.behind-proxy:false}")
    private boolean behindProxy;

    @Value("${app.trusted-proxy-ips:}")
    private String trustedProxyIps;

    /**
     * Resolves the real client IP address
     */
    public String resolveClientIp(HttpServletRequest request) {
        // If not behind proxy, use direct connection IP
        if (!behindProxy) {
            return request.getRemoteAddr();
        }

        // ✅ Check multiple proxy headers in order of preference
        String ip = getHeaderIp(request, "CF-Connecting-IP");      // Cloudflare
        if (ip == null) {
            ip = getHeaderIp(request, "X-Real-IP");               // Nginx
        }
        if (ip == null) {
            ip = getHeaderIp(request, "X-Forwarded-For");         // Standard
        }
        if (ip == null) {
            ip = request.getRemoteAddr();                          // Fallback
        }

        // ✅ Validate IP format
        if (!isValidIp(ip)) {
            log.warn("Invalid IP address format: {}. Using fallback.", ip);
            return request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * Extract IP from header and validate
     */
    private String getHeaderIp(HttpServletRequest request, String headerName) {
        String header = request.getHeader(headerName);
        
        if (header == null || header.isEmpty() || "unknown".equalsIgnoreCase(header)) {
            return null;
        }

        // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
        // Take the first one (leftmost = original client)
        String ip = header.split(",")[0].trim();

        // ✅ If we have trusted proxies, validate that the request came through them
        if (!trustedProxyIps.isEmpty() && headerName.equals("X-Forwarded-For")) {
            String remoteAddr = request.getRemoteAddr();
            if (!isTrustedProxy(remoteAddr)) {
                log.warn("Untrusted proxy {} trying to set X-Forwarded-For to {}. Ignoring.", 
                         remoteAddr, ip);
                return null;
            }
        }

        return ip;
    }

    /**
     * Check if the immediate connection is from a trusted proxy
     */
    private boolean isTrustedProxy(String ip) {
        if (trustedProxyIps.isEmpty()) {
            return true; // No restriction if not configured
        }

        List<String> trusted = Arrays.asList(trustedProxyIps.split(","));
        return trusted.stream()
                .map(String::trim)
                .anyMatch(trustedIp -> trustedIp.equals(ip) || 
                                       ip.startsWith(trustedIp)); // CIDR-like matching
    }

    /**
     * Validate IP address format
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // Check IPv4
        if (IP_PATTERN.matcher(ip).matches()) {
            return true;
        }
        
        // Check IPv6
        if (IPV6_PATTERN.matcher(ip).matches()) {
            return true;
        }
        
        // localhost variants
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "127.0.0.1".equals(ip)) {
            return true;
        }
        
        return false;
    }

    /**
     * Get IP with geolocation hint (for session display)
     */
    public String getClientIpWithLocation(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        
        // If using Cloudflare, get country from header
        String country = request.getHeader("CF-IPCountry");
        if (country != null && !country.isEmpty()) {
            return ip + " (" + country + ")";
        }
        
        return ip;
    }
}

