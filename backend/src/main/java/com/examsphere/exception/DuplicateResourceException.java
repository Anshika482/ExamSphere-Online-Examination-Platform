package com.examsphere.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Duplicate email, student ID or employee ID. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String field, String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_" + field.toUpperCase(), message, Map.of(field, message), null);
    }
}
