package com.examsphere.exception;

import org.springframework.http.HttpStatus;

public class DuplicateSubmissionException extends ApiException {

    public DuplicateSubmissionException(String message) {
        super(HttpStatus.CONFLICT, "ALREADY_SUBMITTED", message);
    }

    public DuplicateSubmissionException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
