package com.voisetu.backend;

import com.voisetu.backend.dto.request.ContactRequest;
import com.voisetu.backend.exception.AppException;
import com.voisetu.backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public contact form endpoint.
 * POST /api/public/contact
 *
 * Rate-limited to 5 submissions per IP per hour via shared {@link RateLimitService}.
 * Structural validation via Bean Validation (@Valid).
 */
@RestController
@RequestMapping("/api/public/contact")
public class ContactController {

    private static final int CONTACT_MAX_PER_HOUR = 5;

    private final JdbcTemplate jdbcTemplate;
    private final RateLimitService rateLimitService;

    public ContactController(JdbcTemplate jdbcTemplate, RateLimitService rateLimitService) {
        this.jdbcTemplate = jdbcTemplate;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody ContactRequest request,
                                                       HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();

        if (!rateLimitService.isAllowed(clientIp, CONTACT_MAX_PER_HOUR, "contact")) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many messages sent. Please try again in an hour.");
        }

        jdbcTemplate.update(
                "INSERT INTO contact_message (name, email, message) VALUES (?, ?, ?)",
                request.name().trim(), request.email().trim(), request.message().trim()
        );

        return ResponseEntity.ok(Map.of("message", "Thanks for reaching out! I'll get back to you soon."));
    }
}
