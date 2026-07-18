package com.voisetu.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
public class SupertonicClient {
    private static final Logger log = LoggerFactory.getLogger(SupertonicClient.class);
    private final RestClient restClient;
    private final String baseUrl;

    public SupertonicClient(@Value("${supertonic.engine.base-url:http://127.0.0.1:7788}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @PostConstruct
    public void checkEngineStatus() {
        try {
            restClient.get().uri("/docs").retrieve().toBodilessEntity();
            log.info("Supertonic engine is reachable at {}", baseUrl);
        } catch (Exception e) {
            log.warn("⚠️ Supertonic engine not reachable at {} — start it with: supertonic serve --host 127.0.0.1 --port 7788");
        }
    }

    /*
     * This request shape is a best guess based on OpenAI-compatible TTS conventions.
     * Before relying on this, start the engine and check http://127.0.0.1:7788/docs 
     * for the real schema, then adjust the request DTO to match exactly.
     */
    public byte[] synthesize(String text, String engineVoiceId, String lang, double speed, int totalSteps) {
        Map<String, Object> requestBody = Map.of(
            "model", engineVoiceId,
            "input", text,
            "voice", engineVoiceId,
            "lang", lang,
            "speed", speed,
            "total_steps", totalSteps
        );

        try {
            ResponseEntity<Resource> response = restClient.post()
                .uri("/v1/audio/speech")
                .body(requestBody)
                .retrieve()
                .toEntity(Resource.class);

            if (response.getBody() != null) {
                return response.getBody().getContentAsByteArray();
            }
            throw new RuntimeException("Supertonic returned empty body");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Supertonic engine unreachable. Is it running on port 7788?");
            throw new EngineUnreachableException("Supertonic engine is offline.");
        } catch (Exception e) {
            log.error("Failed to read audio bytes from Supertonic", e);
            throw new RuntimeException("Failed to read audio bytes from Supertonic", e);
        }
    }

    public static class EngineUnreachableException extends RuntimeException {
        public EngineUnreachableException(String message) {
            super(message);
        }
    }
}
