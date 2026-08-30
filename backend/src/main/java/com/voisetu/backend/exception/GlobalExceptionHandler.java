package com.voisetu.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler — catches all exceptions thrown from controllers and services
 * and converts them to a consistent {@link ApiError} JSON response.
 *
 * Angular reads the {@code code} field to select the appropriate user-facing analogy + quote.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiError.builder()
                        .code(ex.getErrorCode())
                        .status(ex.getStatus().value())
                        .message(ex.getMessage())
                        .build());
    }

    // ── Bean Validation failures (@Valid on @RequestBody) ─────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.debug("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(ApiError.builder()
                        .code("VALIDATION_ERROR")
                        .status(400)
                        .message("Request validation failed")
                        .details(fieldErrors)
                        .build());
    }

    // ── Path variable type mismatches (e.g. /api/generations/abc) ────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.builder()
                        .code("INVALID_PATH_VARIABLE")
                        .status(400)
                        .message("Invalid value for parameter '" + ex.getName() + "': " + ex.getValue())
                        .build());
    }

    // ── Spring Security — access denied ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.builder()
                        .code("ACCESS_DENIED")
                        .status(403)
                        .message("You don't have permission to perform this action.")
                        .build());
    }

    // ── Authentication failures ───────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.builder()
                        .code("INVALID_CREDENTIALS")
                        .status(401)
                        .message("Invalid email or password.")
                        .build());
    }

    // ── Catch-all — unexpected exceptions ─────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.builder()
                        .code("INTERNAL_ERROR")
                        .status(500)
                        .message("An unexpected error occurred. Our team has been notified.")
                        .build());
    }
}
