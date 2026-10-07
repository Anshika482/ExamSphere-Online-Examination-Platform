package com.examsphere.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public ForbiddenOperationException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
