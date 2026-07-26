package com.voisetu.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;

@RestController
@RequestMapping("/api/public/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final TtsEngineManager ttsEngineManager;

    public HealthController(JdbcTemplate jdbcTemplate, TtsEngineManager ttsEngineManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.ttsEngineManager = ttsEngineManager;
    }

    @GetMapping
    public ResponseEntity<?> health() {
        boolean dbOk = true;
        try {
            jdbcTemplate.execute("SELECT 1");
        } catch (Exception e) {
            dbOk = false;
        }

        boolean ttsOk = ttsEngineManager.isEngineReachable();

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = rb.getUptime();

        return ResponseEntity.ok(Map.of(
                "status", (dbOk && ttsOk) ? "ok" : "degraded",
                "database", dbOk ? "connected" : "down",
                "ttsEngine", ttsOk ? "reachable" : "unreachable",
                "uptimeSeconds", uptimeMs / 1000
        ));
    }
}
