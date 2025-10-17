package com.realtime.communication.shared.domain.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message, "NOT_FOUND");
    }

    public NotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier), "NOT_FOUND");
    }
}

