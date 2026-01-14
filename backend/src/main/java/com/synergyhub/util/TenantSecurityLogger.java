package com.synergyhub.util;

import com.synergyhub.security.OrganizationContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TenantSecurityLogger {

    /**
     * Logs a warning when repository methods that can leak tenant data are called
     * without organization context being set.
     */
    @Before("execution(* com.synergyhub.repository.*Repository.*(..)) && " +
            "!execution(* com.synergyhub.repository.*Repository.*InOrganization(..))")
    public void logPotentialTenantLeakage(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String repositoryName = joinPoint.getTarget().getClass().getSimpleName();
        
        // Skip certain methods that are safe without context (like count(), findAll(), etc)
        if (isSafeWithoutContext(methodName)) {
            return;
        }
        
        // Check if organization context is set
        boolean hasOrgContext = OrganizationContext.hasCurrentOrgId();
        
        if (!hasOrgContext) {
            log.warn("POTENTIAL TENANT LEAKAGE: Repository method {}.{}() called without organization context. " +
                    "This could leak data across organizations. Consider using *InOrganization() method instead.",
                    repositoryName, methodName);
        }
    }
    
    private boolean isSafeWithoutContext(String methodName) {
        // These methods don't filter by data and are safe/don't leak tenant data
        return methodName.equals("count") || 
               methodName.equals("findAll") ||
               methodName.equals("save") ||
               methodName.equals("saveAll") ||
               methodName.equals("delete") ||
               methodName.equals("deleteAll") ||
               methodName.equals("deleteById") ||
               methodName.startsWith("exists") ||
               methodName.startsWith("count");
    }
}