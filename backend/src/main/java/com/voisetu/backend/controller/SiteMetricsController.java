package com.voisetu.backend.controller;

import com.voisetu.backend.repository.AppUserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * SiteMetricsController — admin-only business analytics endpoint.
 *
 * View at: GET /api/admin/metrics  (requires admin JWT)
 *
 * Returns all platform KPIs:
 *   - Total & daily unique visitors (sessions + anonymous IDs)
 *   - Total registered users and new signups per day
 *   - Total + daily successful generations and characters synthesized
 *   - Popular voices, quality settings, languages used
 *   - Contact messages count
 *
 * Use these metrics to decide whether to add premium TTS models.
 */
@RestController
@RequestMapping("/api/admin/metrics")
public class SiteMetricsController {

    private final JdbcTemplate jdbc;
    private final AppUserRepository userRepo;

    public SiteMetricsController(JdbcTemplate jdbc, AppUserRepository userRepo) {
        this.jdbc = jdbc;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<?> getMetrics(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        var userOpt = userRepo.findByEmail(auth.getName());
        if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin only"));
        }

        // ── Visitor stats ────────────────────────────────────────────────
        long totalSessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics_session", Long.class);

        long uniqueVisitors = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT anonymous_id) FROM analytics_session", Long.class);

        long todaySessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics_session WHERE created_at >= CURRENT_DATE", Long.class);

        long thisWeekSessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics_session WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'", Long.class);

        long thisMonthSessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics_session WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'", Long.class);

        // ── Page view stats ───────────────────────────────────────────────
        long totalPageViews = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics_event WHERE event_name = 'page_view'", Long.class);

        long todayPageViews = jdbc.queryForObject(
                "SELECT COUNT(*) FROM analytics_event WHERE event_name = 'page_view' AND created_at >= CURRENT_DATE", Long.class);

        // ── User stats ───────────────────────────────────────────────────
        long totalUsers = jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);

        long newUsersToday = jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE created_at >= CURRENT_DATE", Long.class);

        long newUsersThisWeek = jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'", Long.class);

        long newUsersThisMonth = jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'", Long.class);

        // ── Generation stats ─────────────────────────────────────────────
        long totalGenerations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'success'", Long.class);

        long totalChars = jdbc.queryForObject(
                "SELECT COALESCE(SUM(char_count), 0) FROM generation WHERE status = 'success'", Long.class);

        long generationsToday = jdbc.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'success' AND created_at >= CURRENT_DATE", Long.class);

        long generationsThisWeek = jdbc.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'success' AND created_at >= CURRENT_DATE - INTERVAL '7 days'", Long.class);

        long generationsThisMonth = jdbc.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'success' AND created_at >= CURRENT_DATE - INTERVAL '30 days'", Long.class);

        long failedGenerations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM generation WHERE status = 'failed'", Long.class);

        // ── Quality preference breakdown ──────────────────────────────────
        List<Map<String, Object>> qualityStats = jdbc.queryForList(
                "SELECT sm.quality_steps, COUNT(*) as count FROM synthesis_metric sm " +
                "GROUP BY sm.quality_steps ORDER BY sm.quality_steps");

        // ── Top voices used ───────────────────────────────────────────────
        List<Map<String, Object>> topVoices = jdbc.queryForList(
                "SELECT v.display_name, v.engine_voice_id, COUNT(*) as generation_count " +
                "FROM generation g JOIN voice v ON g.voice_id = v.id " +
                "WHERE g.status = 'success' " +
                "GROUP BY v.display_name, v.engine_voice_id " +
                "ORDER BY generation_count DESC LIMIT 10");

        // ── Device breakdown ──────────────────────────────────────────────
        List<Map<String, Object>> deviceStats = jdbc.queryForList(
                "SELECT COALESCE(device_type, 'unknown') as device_type, COUNT(*) as sessions " +
                "FROM analytics_session GROUP BY device_type ORDER BY sessions DESC");

        // ── Browser breakdown ─────────────────────────────────────────────
        List<Map<String, Object>> browserStats = jdbc.queryForList(
                "SELECT COALESCE(browser, 'unknown') as browser, COUNT(*) as sessions " +
                "FROM analytics_session GROUP BY browser ORDER BY sessions DESC");

        // ── Popular pages ─────────────────────────────────────────────────
        List<Map<String, Object>> popularPages = jdbc.queryForList(
                "SELECT route, COUNT(*) as views FROM analytics_event " +
                "WHERE event_name = 'page_view' AND route IS NOT NULL " +
                "GROUP BY route ORDER BY views DESC LIMIT 15");

        // ── Contact messages ──────────────────────────────────────────────
        long contactMessages = jdbc.queryForObject(
                "SELECT COUNT(*) FROM contact_message", Long.class);

        // ── Daily sessions trend (last 14 days) ───────────────────────────
        List<Map<String, Object>> dailySessionTrend = jdbc.queryForList(
                "SELECT DATE(created_at) as day, COUNT(*) as sessions, " +
                "COUNT(DISTINCT anonymous_id) as unique_visitors " +
                "FROM analytics_session " +
                "WHERE created_at >= CURRENT_DATE - INTERVAL '14 days' " +
                "GROUP BY day ORDER BY day DESC");

        // ── Daily generation trend (last 14 days) ─────────────────────────
        List<Map<String, Object>> dailyGenTrend = jdbc.queryForList(
                "SELECT DATE(created_at) as day, COUNT(*) as generations, " +
                "COALESCE(SUM(char_count), 0) as chars_synthesized " +
                "FROM generation WHERE status = 'success' " +
                "AND created_at >= CURRENT_DATE - INTERVAL '14 days' " +
                "GROUP BY day ORDER BY day DESC");

        return ResponseEntity.ok(Map.of(
            "visitors", Map.of(
                "totalSessions", totalSessions,
                "uniqueVisitors", uniqueVisitors,
                "today", todaySessions,
                "last7days", thisWeekSessions,
                "last30days", thisMonthSessions
            ),
            "pageViews", Map.of(
                "total", totalPageViews,
                "today", todayPageViews,
                "popularPages", popularPages
            ),
            "users", Map.of(
                "total", totalUsers,
                "newToday", newUsersToday,
                "newLast7days", newUsersThisWeek,
                "newLast30days", newUsersThisMonth
            ),
            "generations", Map.of(
                "total", totalGenerations,
                "totalChars", totalChars,
                "today", generationsToday,
                "last7days", generationsThisWeek,
                "last30days", generationsThisMonth,
                "failed", failedGenerations,
                "topVoices", topVoices,
                "qualityBreakdown", qualityStats
            ),
            "engagement", Map.of(
                "deviceBreakdown", deviceStats,
                "browserBreakdown", browserStats,
                "contactMessages", contactMessages
            ),
            "trends", Map.of(
                "dailySessions", dailySessionTrend,
                "dailyGenerations", dailyGenTrend
            )
        ));
    }
}
