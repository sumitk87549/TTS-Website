package com.voisetu.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Manages the lifecycle of the external Voisetu TTS FastAPI service.
 *
 * The service (tts-service/main.py) is started independently via:
 *   cd tts-service && ../supertonic-env/bin/uvicorn main:app --port 8000
 *
 * This manager only does a health-check at Spring startup / shutdown and
 * logs clear guidance if the service is not reachable.
 * It does NOT spawn a subprocess — that keeps Spring Boot startup fast and
 * avoids PID-management complexity during development.
 */
@Service
public class TtsEngineManager implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TtsEngineManager.class);

    @Value("${supertonic.engine.base-url:http://127.0.0.1:8000}")
    private String baseUrl;

    private boolean running = false;

    @Override
    public void start() {
        log.info("Checking Voisetu TTS service at {} ...", baseUrl);
        try {
            RestClient.create(baseUrl)
                    .get().uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            log.info("✅  Voisetu TTS service is READY at {}", baseUrl);
        } catch (Exception e) {
            log.warn("⚠️  Voisetu TTS service NOT reachable at {}.", baseUrl);
            log.warn("    Start it before generating audio:");
            log.warn("    cd tts-service && ../supertonic-env/bin/uvicorn main:app --host 127.0.0.1 --port 8000");
            log.warn("    Or use the convenience script:  ./tts-service/start-tts-service.sh");
        }
        running = true;
    }

    @Override
    public void stop() {
        log.info("TtsEngineManager stopped. The external TTS service continues running independently.");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Run after most beans are wired, but before web server starts taking traffic
        return Integer.MAX_VALUE - 100;
    }
}
