package com.voisetu.backend;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;
    private final AppUserRepository userRepository;

    public AdminController(JdbcTemplate jdbcTemplate, AppUserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(Authentication auth) {
        AppUser user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (!user.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required."));
        }

        long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        long totalGenerations = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM generation WHERE status = 'success'", Long.class);
        long totalChars = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(char_count), 0) FROM generation WHERE status = 'success'", Long.class);

        List<Map<String, Object>> interestBreakdown = jdbcTemplate.queryForList(
                "SELECT would_pay, COUNT(*) AS count, " +
                "ROUND(AVG(suggested_price_inr) FILTER (WHERE suggested_price_inr IS NOT NULL), 0) AS avg_price_inr " +
                "FROM interest_signal GROUP BY would_pay ORDER BY would_pay"
        );

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalSuccessfulGenerations", totalGenerations,
                "totalCharactersGenerated", totalChars,
                "interestSignalBreakdown", interestBreakdown
        ));
    }
}
