package com.realtime.communication.shared.domain.exception;

/**
 * Exception thrown when authentication fails or user is not authorized.
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }

    public UnauthorizedException() {
        super("Unauthorized access", "UNAUTHORIZED");
    }
}

