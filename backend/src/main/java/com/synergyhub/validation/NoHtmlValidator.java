package com.synergyhub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Validator implementation for @NoHtml annotation.
 * Detects and prevents HTML tags, script injections, and XSS patterns.
 */
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {
    
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder().toFactory();
    
    @Override
    public void initialize(NoHtml constraintAnnotation) {
        // No initialization required
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // Sanitize and compare - if different, HTML was present
        String sanitized = POLICY.sanitize(value);
        return value.equals(sanitized);
    }
}
