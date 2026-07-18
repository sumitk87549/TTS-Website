package com.voisetu.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voices")
public class VoiceController {
    
    private final JdbcTemplate jdbcTemplate;

    public VoiceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getVoices() {
        return jdbcTemplate.queryForList("SELECT id, engine_voice_id, display_name, gender, style_tag FROM voice WHERE is_available = true ORDER BY gender, id");
    }
}
