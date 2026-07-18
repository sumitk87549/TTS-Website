package com.voisetu.backend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/public/contact")
public class ContactController {

    private final JdbcTemplate jdbcTemplate;

    // Same simple in-memory per-IP rate limiter as the public TTS endpoint: 5/hour
    private final Map<String, RateLimitEntry> rateLimits = new ConcurrentHashMap<>();

    private static class RateLimitEntry {
        AtomicInteger count = new AtomicInteger(0);
        Instant resetTime = Instant.now().plus(1, ChronoUnit.HOURS);
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public ContactController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        // Evict expired windows and apply rate limit
        rateLimits.entrySet().removeIf(e -> e.getValue().resetTime.isBefore(Instant.now()));
        RateLimitEntry entry = rateLimits.computeIfAbsent(ip, k -> new RateLimitEntry());
        if (entry.count.incrementAndGet() > 5) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many messages sent. Please try again in an hour."));
        }

        String name    = body.getOrDefault("name", "").trim();
        String email   = body.getOrDefault("email", "").trim();
        String message = body.getOrDefault("message", "").trim();

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email, and message are all required."));
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "That doesn't look like a valid email address."));
        }
        if (message.length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is too long (max 2000 characters)."));
        }

        jdbcTemplate.update(
                "INSERT INTO contact_message (name, email, message) VALUES (?, ?, ?)",
                name, email, message
        );

        return ResponseEntity.ok(Map.of("message", "Thanks for reaching out! I'll get back to you soon."));
    }
}
