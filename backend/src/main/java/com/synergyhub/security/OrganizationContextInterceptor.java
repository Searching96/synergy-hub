package com.synergyhub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that extracts organization ID from URL paths and sets OrganizationContext.
 * 
 * This interceptor looks for organization ID in:
 * 1. Path variables: /api/organizations/{orgId}/...
 * 2. Headers: X-Organization-ID
 * 3. Query parameters: ?organizationId=...
 * 
 * Priority order: Path variable > Header > Query parameter
 */
@Component
@Slf4j
public class OrganizationContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long orgId = extractOrganizationId(request);
        
        if (orgId != null) {
            log.debug("Setting organization context: {}", orgId);
            OrganizationContext.setcurrentOrgId(orgId);
        } else {
            log.debug("No organization ID found in request");
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear organization context to prevent leakage between requests
        OrganizationContext.clear();
    }

    /**
     * Extract organization ID from the request using multiple strategies.
     */
    private Long extractOrganizationId(HttpServletRequest request) {
        Long orgId = null;
        
        // 1. Try to extract from path variables (e.g., /api/organizations/{orgId}/...)
        orgId = extractFromPath(request);
        
        // 2. Try to extract from header (X-Organization-ID)
        if (orgId == null) {
            orgId = extractFromHeader(request);
        }
        
        // 3. Try to extract from query parameter (?organizationId=...)
        if (orgId == null) {
            orgId = extractFromQueryParam(request);
        }
        
        return orgId;
    }

    /**
     * Extract organization ID from URL path.
     * Supports patterns like:
     * - /api/organizations/{orgId}/...
     * - /organizations/{orgId}/...
     */
    private Long extractFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Pattern: /api/organizations/{orgId}/...
        if (path.contains("/organizations/")) {
            String[] parts = path.split("/organizations/");
            if (parts.length > 1) {
                String afterOrg = parts[1];
                // Extract the next segment which should be the orgId
                String[] segments = afterOrg.split("/");
                if (segments.length > 0 && !segments[0].isEmpty()) {
                    try {
                        return Long.parseLong(segments[0]);
                    } catch (NumberFormatException e) {
                        log.debug("Path segment after /organizations/ is not a number: {}", segments[0]);
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Extract organization ID from X-Organization-ID header.
     */
    private Long extractFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("X-Organization-ID");
        if (headerValue != null && !headerValue.trim().isEmpty()) {
            try {
                return Long.parseLong(headerValue.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Organization-ID header value: {}", headerValue);
            }
        }
        return null;
    }

    /**
     * Extract organization ID from query parameter.
     */
    private Long extractFromQueryParam(HttpServletRequest request) {
        String paramValue = request.getParameter("organizationId");
        if (paramValue != null && !paramValue.trim().isEmpty()) {
            try {
                return Long.parseLong(paramValue.trim());
            } catch (NumberFormatException e) {
                log.debug("Invalid organizationId query parameter: {}", paramValue);
            }
        }
        return null;
    }
}