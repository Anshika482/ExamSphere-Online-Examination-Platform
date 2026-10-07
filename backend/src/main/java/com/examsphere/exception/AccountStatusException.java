package com.examsphere.exception;

import org.springframework.http.HttpStatus;

public class AccountStatusException extends ApiException {

    public AccountStatusException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_STATUS", message);
    }

    public AccountStatusException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
