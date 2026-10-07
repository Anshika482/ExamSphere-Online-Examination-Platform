package com.examsphere.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /** A validation failure tied to one form field. */
    public BadRequestException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", Map.of(field, message), null);
    }
}
