package com.campusconnect.exception;

/** Thrown when a business rule is violated. Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
