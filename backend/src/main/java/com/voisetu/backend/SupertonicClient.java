package com.voisetu.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Client for the Voisetu FastAPI TTS service (supertonic-3) on port 8000.
 *
 * Uses java.net.http.HttpClient (JDK 11+) with a hand-built JSON body.
 * This avoids any RestClient / MessageConverter serialisation issues
 * while keeping zero extra Maven dependencies.
 */
@Component
public class SupertonicClient {

    private static final Logger log = LoggerFactory.getLogger(SupertonicClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;

    public SupertonicClient(
            @Value("${supertonic.engine.base-url:http://127.0.0.1:8000}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)  // uvicorn doesn't support HTTP/2
                .build();
    }

    @PostConstruct
    public void checkEngineStatus() {
        try {
            RestClient.create(baseUrl).get().uri("/health").retrieve().toBodilessEntity();
            log.info("✅  Voisetu TTS service is reachable at {}", baseUrl);
        } catch (Exception e) {
            log.warn("⚠️  Voisetu TTS service not reachable at {} — start tts-service/start-tts-service.sh first!", baseUrl);
        }
    }

    /**
     * Synthesise speech via POST /synthesize.
     *
     * @param text       Text to synthesise (Hindi / English / Hinglish)
     * @param voiceId    Voice preset ID: M1–M5 or F1–F5
     * @param lang       Language code: "hi", "en", or "na" (auto/Hinglish)
     * @param speed      Speed multiplier 0.7 – 2.0
     * @param totalSteps Diffusion steps 1 – 40
     * @return Raw WAV bytes
     */
    public byte[] synthesize(String text, String voiceId, String lang,
                             double speed, int totalSteps) {

        // Build JSON manually — the payload is a flat object with primitives only,
        // so escaping the text field is all that's needed.
        String safeText    = escapeJson(text != null    ? text    : "");
        String safeVoiceId = escapeJson(voiceId != null ? voiceId : "M1");
        String safeLang    = escapeJson(lang != null    ? lang    : "na");

        String json = String.format(
            "{\"text\":\"%s\",\"voice_id\":\"%s\",\"lang\":\"%s\"," +
            "\"speed\":%.4f,\"total_steps\":%d,\"silence_duration\":0.3}",
            safeText, safeVoiceId, safeLang, speed, totalSteps
        );

        log.debug("POST {}/synthesize → {}", baseUrl, json.substring(0, Math.min(json.length(), 120)));

        try {
            byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/synthesize"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "audio/wav, */*")
                    .timeout(Duration.ofSeconds(120))   // CPU TTS can be slow
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                log.info("✅  TTS OK — {} bytes returned (voice={} lang={} speed={} steps={})",
                        response.body().length, voiceId, lang, speed, totalSteps);
                return response.body();
            }

            String errorBody = new String(response.body(), StandardCharsets.UTF_8);
            log.error("TTS service HTTP {}: {}", response.statusCode(), errorBody);

            if (response.statusCode() >= 500 || response.statusCode() == 503) {
                throw new EngineUnreachableException("TTS service error " + response.statusCode() + ": " + errorBody);
            }
            throw new RuntimeException("TTS service error " + response.statusCode() + ": " + errorBody);

        } catch (EngineUnreachableException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            log.warn("Cannot connect to TTS service at {}", baseUrl);
            throw new EngineUnreachableException("Voisetu TTS engine is offline — start tts-service/start-tts-service.sh");
        } catch (Exception e) {
            log.error("Failed to synthesise audio: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to synthesise audio: " + e.getMessage(), e);
        }
    }

    /** Minimal JSON string escaping — handles the characters likely in Hindi/English text. */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static class EngineUnreachableException extends RuntimeException {
        public EngineUnreachableException(String message) { super(message); }
    }
}
