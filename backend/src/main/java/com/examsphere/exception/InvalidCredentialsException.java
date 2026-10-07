package com.examsphere.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }

    public InvalidCredentialsException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
