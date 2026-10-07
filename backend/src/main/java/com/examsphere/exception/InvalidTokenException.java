package com.examsphere.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", message);
    }

    public InvalidTokenException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
