package com.examsphere.dto;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Uniform error body. "errors" maps field names to messages for validation failures. */
public record ErrorResponse(String message, int status, String code, Map<String, String> errors,
                            Long retryAfterSeconds, String timestamp) {

    public static ErrorResponse of(HttpStatus status, String code, String message, Map<String, String> errors,
                                   Long retryAfterSeconds) {
        return new ErrorResponse(message, status.value(), code, errors, retryAfterSeconds, LocalDateTime.now().toString());
    }
}
