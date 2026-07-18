package com.voisetu.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/public/tts")
public class TtsController {
    
    private static final Logger log = LoggerFactory.getLogger(TtsController.class);
    private final SupertonicClient supertonicClient;
    private final JdbcTemplate jdbcTemplate;

    // Simple in-memory rate limiting: 5 requests per IP per hour
    private final Map<String, RateLimitState> rateLimits = new ConcurrentHashMap<>();

    private static class RateLimitState {
        AtomicInteger count = new AtomicInteger(0);
        Instant resetTime = Instant.now().plus(1, ChronoUnit.HOURS);
    }

    public TtsController(SupertonicClient supertonicClient, JdbcTemplate jdbcTemplate) {
        this.supertonicClient = supertonicClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/voices")
    public List<Map<String, Object>> getVoices() {
        return jdbcTemplate.queryForList("SELECT id, engine_voice_id, display_name, gender, style_tag FROM voice WHERE is_available = true ORDER BY id");
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(
            @RequestBody TtsRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = httpRequest.getRemoteAddr();
        
        // Rate limiting
        rateLimits.entrySet().removeIf(entry -> entry.getValue().resetTime.isBefore(Instant.now()));
        RateLimitState state = rateLimits.computeIfAbsent(clientIp, k -> new RateLimitState());
        
        if (state.count.incrementAndGet() > 5) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Rate limit exceeded. Maximum 5 preview requests per hour."));
        }

        // Validation
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text cannot be empty."));
        }
        if (request.getText().length() > 300) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text exceeds maximum length of 300 characters."));
        }
        if (request.getEngineVoiceId() == null || request.getEngineVoiceId().trim().isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of("error", "Voice ID is required."));
        }

        try {
            // Find voice DB id
            List<Map<String, Object>> voices = jdbcTemplate.queryForList(
                "SELECT id FROM voice WHERE engine_voice_id = ?", 
                request.getEngineVoiceId()
            );
            
            if (voices.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid voice ID."));
            }
            
            Long voiceId = ((Number) voices.get(0).get("id")).longValue();

            // Call Supertonic
            byte[] audioBytes = supertonicClient.synthesize(
                request.getText(), 
                request.getEngineVoiceId(), 
                "hi", // lang hardcoded for now
                1.0,  // speed
                8     // totalSteps
            );

            // Save generation record
            jdbcTemplate.update(
                "INSERT INTO generation (voice_id, input_text, char_count, status) VALUES (?, ?, ?, ?)",
                voiceId,
                request.getText(),
                request.getText().length(),
                "success"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            
            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

        } catch (SupertonicClient.EngineUnreachableException e) {
            log.warn("TTS request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "The voice engine seems to be asleep right now — try again in a moment."));
        } catch (Exception e) {
            log.error("TTS Engine failure", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "An unexpected error occurred during synthesis."));
        }
    }
}
