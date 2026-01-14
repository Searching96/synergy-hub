package com.synergyhub.security;

import com.synergyhub.exception.ContextMissingException;

public class OrganizationContext {
    private static final ThreadLocal<Long> currentOrgId = new ThreadLocal<>();

    public static void setcurrentOrgId(Long orgId) {
        currentOrgId.set(orgId);
    }

    public static Long getcurrentOrgId() {
        Long orgId = currentOrgId.get();
        if (orgId == null) {
            throw new ContextMissingException("Organization context is not set. Ensure organization context is set before calling this method.");
        }
        return orgId;
    }
    
    public static Long getcurrentOrgIdOrNull() {
        return currentOrgId.get();
    }
    
    public static boolean hasCurrentOrgId() {
        return currentOrgId.get() != null;
    }

    public static void clear() {
        currentOrgId.remove();
    }
}

