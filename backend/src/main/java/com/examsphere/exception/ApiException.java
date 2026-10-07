package com.examsphere.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base class of every business error. Each subclass fixes the HTTP status;
 * the optional code lets the frontend react to a specific situation.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> errors;
    private final Long retryAfterSeconds;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, String> errors, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
