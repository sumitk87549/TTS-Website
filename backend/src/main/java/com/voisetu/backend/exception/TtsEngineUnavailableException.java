package com.voisetu.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the TTS engine is unreachable, overloaded, or the semaphore queue is full. */
public class TtsEngineUnavailableException extends AppException {

    public TtsEngineUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "TTS_ENGINE_UNAVAILABLE", message);
    }

    public TtsEngineUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "TTS_ENGINE_UNAVAILABLE", message, cause);
    }

    /** Used when the concurrency semaphore queue is full — transient, user should retry soon. */
    public static TtsEngineUnavailableException busy() {
        return new TtsEngineUnavailableException(
                "The voice generation studio is at full capacity right now. Please retry in a moment.");
    }
}
