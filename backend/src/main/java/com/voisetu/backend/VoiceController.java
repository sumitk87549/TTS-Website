package com.voisetu.backend;

import com.voisetu.backend.dto.response.VoiceResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Voice listing endpoint (authenticated).
 * GET /api/voices — returns available voices from database.
 *
 * This endpoint is intentionally public-friendly (voices aren't sensitive),
 * but it's kept under /api (auth-required) to count authenticated usage.
 * The public preview endpoint also exposes voices via /api/public/tts/voices.
 */
@RestController
@RequestMapping("/api/voices")
public class VoiceController {

    private final JdbcTemplate jdbcTemplate;

    public VoiceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<VoiceResponse> getVoices() {
        return jdbcTemplate.queryForList(
                "SELECT id, engine_voice_id, display_name, gender, style_tag " +
                "FROM voice WHERE is_available = true ORDER BY gender, id")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VoiceResponse toResponse(Map<String, Object> row) {
        return new VoiceResponse(
                ((Number) row.get("id")).longValue(),
                (String) row.get("engine_voice_id"),
                (String) row.get("display_name"),
                (String) row.get("gender"),
                (String) row.get("style_tag")
        );
    }
}
