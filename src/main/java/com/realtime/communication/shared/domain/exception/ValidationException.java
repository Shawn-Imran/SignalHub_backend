package com.realtime.communication.shared.domain.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when domain validation fails.
 * Contains field-specific validation errors.
 */
public class ValidationException extends DomainException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String field, String error) {
        super("Validation failed for field: " + field, "VALIDATION_ERROR");
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(field, error);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void addFieldError(String field, String error) {
        fieldErrors.put(field, error);
    }
}

