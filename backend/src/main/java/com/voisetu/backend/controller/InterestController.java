package com.voisetu.backend.controller;

import com.voisetu.backend.dto.request.InterestRequest;
import com.voisetu.backend.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Monetisation interest signal endpoint.
 * POST /api/interest — authenticated users indicate willingness to pay.
 */
@RestController
@RequestMapping("/api/interest")
public class InterestController {

    private final AuthenticatedUserService authenticatedUserService;
    private final JdbcTemplate jdbcTemplate;

    public InterestController(AuthenticatedUserService authenticatedUserService,
                              JdbcTemplate jdbcTemplate) {
        this.authenticatedUserService = authenticatedUserService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submitInterest(
            Authentication auth,
            @Valid @RequestBody InterestRequest request) {

        Long userId = authenticatedUserService.userId(auth);

        jdbcTemplate.update(
                "INSERT INTO interest_signal (user_id, would_pay, suggested_price_inr, comment) " +
                "VALUES (?, ?, ?, ?)",
                userId, request.wouldPay(), request.suggestedPriceInr(), request.comment()
        );

        return ResponseEntity.ok(Map.of("message", "Thank you for sharing your thoughts!"));
    }
}
