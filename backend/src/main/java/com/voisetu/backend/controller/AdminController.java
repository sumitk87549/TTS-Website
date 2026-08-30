package com.voisetu.backend.controller;

import com.voisetu.backend.security.SecurityConfig;

import com.voisetu.backend.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only analytics and management endpoints.
 *
 * All methods are protected by {@code @PreAuthorize("hasRole('ADMIN')")} — Spring Security
 * enforces this via method-level security (requires {@code @EnableMethodSecurity} on SecurityConfig).
 *
 * No more manual isAdmin() DB lookups on every endpoint.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;

    public AdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        long totalGenerations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'success'", Long.class);
        long failedGenerations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status != 'success'", Long.class);
        long totalChars = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(char_count), 0) FROM generation WHERE status = 'success'", Long.class);
        long todayGenerations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE created_at >= current_date AND status = 'success'", Long.class);
        long todayActiveUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM analytics_session WHERE created_at >= current_date AND user_id IS NOT NULL", Long.class);
        Double avgGenTime = jdbcTemplate.queryForObject(
                "SELECT AVG(synthesis_ms) / 1000.0 FROM generation WHERE status = 'success' AND synthesis_ms IS NOT NULL", Double.class);
        List<Map<String, Object>> interestBreakdown = jdbcTemplate.queryForList(
                "SELECT would_pay, COUNT(*) AS count, " +
                "ROUND(AVG(suggested_price_inr) FILTER (WHERE suggested_price_inr IS NOT NULL), 0) AS avg_price " +
                "FROM interest_signal GROUP BY would_pay ORDER BY would_pay");

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalGenerations", totalGenerations,
                "failedGenerations", failedGenerations,
                "totalCharacters", totalChars,
                "todayGenerations", todayGenerations,
                "todayActiveUsers", todayActiveUsers,
                "avgGenerationTimeSec", avgGenTime != null ? avgGenTime : 0.0,
                "interestBreakdown", interestBreakdown
        ));
    }

    @GetMapping("/top-voices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> topVoices() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
                "SELECT v.display_name, v.engine_voice_id, COUNT(g.id) as gen_count " +
                "FROM voice v LEFT JOIN generation g ON v.id = g.voice_id " +
                "GROUP BY v.id, v.display_name, v.engine_voice_id " +
                "ORDER BY gen_count DESC LIMIT 20"));
    }

    @GetMapping("/recent-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> recentUsers() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
                "SELECT u.id, u.display_name, u.email, u.created_at, " +
                "(SELECT COUNT(*) FROM generation g WHERE g.user_id = u.id) as generation_count, " +
                "(SELECT COALESCE(SUM(char_count), 0) FROM generation g WHERE g.user_id = u.id AND g.status='success') as total_chars " +
                "FROM app_user u ORDER BY u.created_at DESC LIMIT 50"));
    }

    @GetMapping("/daily-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> dailyStats() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
                "SELECT DATE(created_at) as day, COUNT(*) as generations, " +
                "COUNT(DISTINCT user_id) as unique_users, " +
                "COALESCE(SUM(char_count), 0) as total_chars " +
                "FROM generation WHERE status = 'success' AND created_at >= NOW() - INTERVAL '30 days' " +
                "GROUP BY DATE(created_at) ORDER BY day DESC"));
    }

    @GetMapping("/contacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> contacts() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
                "SELECT * FROM contact_message ORDER BY created_at DESC LIMIT 100"));
    }

    @GetMapping("/analytics-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> analyticsSummary() {
        long totalEvents7d = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_event WHERE created_at >= NOW() - INTERVAL '7 days'", Long.class);
        long uniqueVisitors7d = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT anonymous_id) FROM analytics_session WHERE created_at >= NOW() - INTERVAL '7 days'", Long.class);
        List<Map<String, Object>> eventCounts = jdbcTemplate.queryForList(
                "SELECT event_name, COUNT(*) as count FROM analytics_event " +
                "WHERE created_at >= NOW() - INTERVAL '7 days' " +
                "GROUP BY event_name ORDER BY count DESC LIMIT 10");

        return ResponseEntity.ok(Map.of(
                "totalEvents7d", totalEvents7d,
                "uniqueVisitors7d", uniqueVisitors7d,
                "eventCounts", eventCounts
        ));
    }
}
