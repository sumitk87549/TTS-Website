package com.voisetu.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tts")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);
    private final SupertonicClient supertonicClient;
    private final DashboardRepository dashboardRepository;
    private final AppUserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.storage.audio-dir:backend-data/audio}")
    private String audioDir;

    @Value("${app.usage.daily-limit:5000}")
    private int dailyLimit;

    @Value("${app.usage.max-request-chars:1000}")
    private int maxChars;

    public GenerationController(SupertonicClient supertonicClient, DashboardRepository dashboardRepository, AppUserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.supertonicClient = supertonicClient;
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().id();
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(Authentication auth, @RequestBody Map<String, Object> request) {
        Long userId = getUserId(auth);
        String text = (String) request.get("text");
        String engineVoiceId = (String) request.get("engineVoiceId");
        Number projectIdNum = (Number) request.get("projectId");
        Long projectId = projectIdNum != null ? projectIdNum.longValue() : null;

        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text cannot be empty."));
        }
        if (text.length() > maxChars) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text exceeds maximum length of " + maxChars + " characters."));
        }

        Map<String, Object> usage = dashboardRepository.getUsageToday(userId);
        int charactersUsed = ((Number) usage.get("characters_used")).intValue();
        if (charactersUsed + text.length() > dailyLimit) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit of " + dailyLimit + " characters exceeded. Resets tomorrow."));
        }

        List<Map<String, Object>> voices = jdbcTemplate.queryForList("SELECT id FROM voice WHERE engine_voice_id = ?", engineVoiceId);
        if (voices.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid voice ID."));
        }
        Long voiceId = ((Number) voices.get(0).get("id")).longValue();

        Long genId = dashboardRepository.createGeneration(userId, projectId, voiceId, text, text.length());

        try {
            byte[] audioBytes = supertonicClient.synthesize(text, engineVoiceId, "hi", 1.0, 8);
            
            File dir = new File(audioDir, userId.toString());
            if (!dir.exists()) dir.mkdirs();
            File audioFile = new File(dir, genId + ".wav");
            
            try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                fos.write(audioBytes);
            }
            
            dashboardRepository.updateGenerationSuccess(genId, audioFile.getAbsolutePath(), 0.0); // duration mocked for now
            dashboardRepository.upsertUsage(userId, text.length());
            
            return ResponseEntity.ok(Map.of("id", genId, "status", "success"));
        } catch (SupertonicClient.EngineUnreachableException e) {
            log.warn("TTS generation failed: {}", e.getMessage());
            dashboardRepository.updateGenerationFailed(genId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "The voice engine seems to be asleep right now — try again in a moment."));
        } catch (Exception e) {
            log.error("TTS generation failed with unexpected error", e);
            dashboardRepository.updateGenerationFailed(genId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "An unexpected error occurred during synthesis."));
        }
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<?> getAudio(Authentication auth, @PathVariable Long id) {
        Long userId = getUserId(auth);
        Optional<String> path = dashboardRepository.getGenerationAudioPath(userId, id);
        if (path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 if they don't own it or it doesn't exist
        }
        File file = new File(path.get());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.setContentLength(file.length());
        
        return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    }
    
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(Authentication auth, @PathVariable Long id) {
        Long userId = getUserId(auth);
        dashboardRepository.toggleGenerationLike(userId, id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
