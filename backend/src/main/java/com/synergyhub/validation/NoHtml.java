package com.synergyhub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to prevent HTML/XSS injection in text fields.
 * Rejects any input containing HTML tags or potentially dangerous characters.
 */
@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {
    
    String message() default "HTML content is not allowed";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * If true, allows basic formatting characters but still blocks HTML tags.
     * If false (default), strictly validates plain text only.
     */
    boolean allowFormatting() default false;
}
