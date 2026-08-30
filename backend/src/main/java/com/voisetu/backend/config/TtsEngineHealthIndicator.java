package com.voisetu.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Spring Actuator health indicator for the external TTS FastAPI service.
 *
 * Exposed at: GET /actuator/health/ttsEngine
 *
 * If the TTS engine is down, the health response shows "status: DOWN" with details,
 * but the Spring Boot app itself remains UP (degraded operation — public preview fails,
 * authenticated generation still fails, but auth/projects/history still work).
 */
@Component("ttsEngine")
public class TtsEngineHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(TtsEngineHealthIndicator.class);

    @Value("${supertonic.engine.base-url:http://127.0.0.1:8000}")
    private String ttsBaseUrl;

    @Override
    public Health health() {
        try {
            @SuppressWarnings("unchecked")
            var response = RestClient.create(ttsBaseUrl)
                    .get()
                    .uri("/health")
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Object status = response.getBody() != null ? response.getBody().get("status") : "unknown";
                Object ready  = response.getBody() != null ? response.getBody().get("ready")  : false;
                return Health.up()
                        .withDetail("url", ttsBaseUrl)
                        .withDetail("engineStatus", status)
                        .withDetail("modelReady", ready)
                        .build();
            }

            return Health.down()
                    .withDetail("url", ttsBaseUrl)
                    .withDetail("httpStatus", response.getStatusCode().value())
                    .build();

        } catch (Exception e) {
            log.debug("TTS engine health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", ttsBaseUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
