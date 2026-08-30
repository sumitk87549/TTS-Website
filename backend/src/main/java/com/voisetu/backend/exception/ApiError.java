package com.voisetu.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Structured error response returned by {@link GlobalExceptionHandler} for every error.
 *
 * Angular reads {@code code} to map to a user-friendly analogy + quote.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** Machine-readable error code — used by Angular for UX mapping. */
    private final String code;

    /** HTTP status int. */
    private final int status;

    /** Short human-readable summary (shown only in logs / debug mode). */
    private final String message;

    /**
     * Field-level validation errors: { fieldName → "error description" }.
     * Present only for 400 validation errors.
     */
    private final Map<String, String> details;

    /** ISO-8601 timestamp. */
    @Builder.Default
    private final Instant timestamp = Instant.now();
}
