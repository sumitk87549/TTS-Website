package com.voisetu.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown for business-rule validation failures not caught by Bean Validation (e.g. duplicate email). */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public ValidationException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }
}
