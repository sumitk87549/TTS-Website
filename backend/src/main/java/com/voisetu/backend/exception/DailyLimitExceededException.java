package com.voisetu.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user exceeds their daily character limit. */
public class DailyLimitExceededException extends AppException {

    public DailyLimitExceededException(int limit) {
        super(HttpStatus.TOO_MANY_REQUESTS, "DAILY_LIMIT_EXCEEDED",
                "Daily character limit of " + limit + " characters has been reached. Resets at midnight.");
    }
}
