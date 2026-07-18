package com.voisetu.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interest")
public class InterestController {

    private final AppUserRepository userRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public InterestController(AppUserRepository userRepository,
                              org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<?> submitInterest(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = userRepository.findByEmail(auth.getName()).orElseThrow().id();

        String wouldPay = (String) body.get("wouldPay");
        if (!java.util.Set.of("yes", "no", "maybe").contains(wouldPay)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid value for wouldPay. Use yes, no, or maybe."));
        }

        Number priceNum = (Number) body.get("suggestedPriceInr");
        Integer suggestedPrice = priceNum != null ? priceNum.intValue() : null;
        String comment = (String) body.get("comment");

        jdbcTemplate.update(
                "INSERT INTO interest_signal (user_id, would_pay, suggested_price_inr, comment) " +
                "VALUES (?, ?, ?, ?)",
                userId, wouldPay, suggestedPrice, comment
        );

        return ResponseEntity.ok(Map.of("message", "Thank you for sharing your thoughts!"));
    }
}
