package com.voisetu.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public-facing stats endpoint — no authentication required.
 * Returns aggregate counts for the landing page social proof.
 */
@RestController
@RequestMapping("/api/public/stats")
public class PublicStatsController {

    private final JdbcTemplate jdbcTemplate;

    public PublicStatsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> publicStats() {
        long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        long totalGenerations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'success'", Long.class);

        return Map.of(
                "totalUsers", totalUsers,
                "totalGenerations", totalGenerations
        );
    }
}
