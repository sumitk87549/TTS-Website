package com.voisetu.backend;

import com.voisetu.backend.dto.request.TtsPreviewRequest;
import com.voisetu.backend.dto.response.VoiceResponse;
import com.voisetu.backend.exception.TtsEngineUnavailableException;
import com.voisetu.backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public (unauthenticated) TTS endpoints.
 *
 * GET  /api/public/tts/voices   — list available voices
 * POST /api/public/tts/preview  — generate a short voice preview (rate-limited)
 */
@RestController
@RequestMapping("/api/public/tts")
public class TtsController {

    private static final Logger log = LoggerFactory.getLogger(TtsController.class);
    private static final int PREVIEW_MAX_PER_HOUR = 5;

    private final SupertonicClient supertonicClient;
    private final JdbcTemplate jdbcTemplate;
    private final RateLimitService rateLimitService;

    public TtsController(SupertonicClient supertonicClient,
                         JdbcTemplate jdbcTemplate,
                         RateLimitService rateLimitService) {
        this.supertonicClient = supertonicClient;
        this.jdbcTemplate = jdbcTemplate;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/voices")
    public List<VoiceResponse> getVoices() {
        return jdbcTemplate.queryForList(
                "SELECT id, engine_voice_id, display_name, gender, style_tag " +
                "FROM voice WHERE is_available = true ORDER BY gender, id")
                .stream()
                .map(this::toVoiceResponse)
                .toList();
    }

    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@Valid @RequestBody TtsPreviewRequest request,
                                          HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();

        if (!rateLimitService.isAllowed(clientIp, PREVIEW_MAX_PER_HOUR, "tts-preview")) {
            throw new com.voisetu.backend.exception.AppException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Too many preview requests. Maximum " + PREVIEW_MAX_PER_HOUR + " per hour."
            );
        }

        // Resolve voice DB id (best-effort — not fatal if not found)
        List<Map<String, Object>> voices = jdbcTemplate.queryForList(
                "SELECT id FROM voice WHERE engine_voice_id = ?", request.voiceId());
        Long voiceId = voices.isEmpty() ? 1L : ((Number) voices.get(0).get("id")).longValue();

        byte[] audioBytes = supertonicClient.synthesize(
                request.text(),
                request.voiceId(),
                request.resolvedLang(),
                1.0,
                8
        );

        // Record anonymous preview generation
        jdbcTemplate.update(
                "INSERT INTO generation (voice_id, input_text, char_count, status) VALUES (?, ?, ?, ?)",
                voiceId, request.text(), request.text().length(), "success"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
    }

    private VoiceResponse toVoiceResponse(Map<String, Object> row) {
        return new VoiceResponse(
                ((Number) row.get("id")).longValue(),
                (String) row.get("engine_voice_id"),
                (String) row.get("display_name"),
                (String) row.get("gender"),
                (String) row.get("style_tag")
        );
    }
}
