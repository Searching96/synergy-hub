package com.synergyhub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ContextMissingException extends RuntimeException {
    
    public ContextMissingException() {
        super("Organization context is missing. Please set organization context before performing this operation.");
    }
    
    public ContextMissingException(String message) {
        super(message);
    }
    
    public ContextMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}