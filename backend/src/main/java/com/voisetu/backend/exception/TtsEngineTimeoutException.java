package com.voisetu.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the TTS synthesis request exceeds the configured timeout. */
public class TtsEngineTimeoutException extends AppException {

    public TtsEngineTimeoutException() {
        super(HttpStatus.GATEWAY_TIMEOUT, "TTS_ENGINE_TIMEOUT",
                "Voice generation timed out. The text may be too long or the engine is under heavy load.");
    }
}
