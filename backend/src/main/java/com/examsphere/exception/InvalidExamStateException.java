package com.examsphere.exception;

import org.springframework.http.HttpStatus;

public class InvalidExamStateException extends ApiException {

    public InvalidExamStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_EXAM_STATE", message);
    }

    public InvalidExamStateException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
