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

    public GenerationController(SupertonicClient supertonicClient,
                                DashboardRepository dashboardRepository,
                                AppUserRepository userRepository,
                                JdbcTemplate jdbcTemplate) {
        this.supertonicClient = supertonicClient;
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().id();
    }

    /**
     * Generate TTS audio for an authenticated user.
     *
     * Returns raw WAV bytes (audio/wav) instead of JSON so that Angular can
     * receive it as a Blob and create an Object URL directly.
     * This avoids the Jackson serialisation issue with spring-boot-starter-webmvc
     * where Map.of() responses hang and never complete.
     *
     * The generation ID is carried in the X-Generation-Id response header.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generate(Authentication auth,
                                      @RequestBody Map<String, Object> request) {
        Long userId = getUserId(auth);
        String text = (String) request.get("text");

        // Accept both voiceId (new) and engineVoiceId (legacy)
        String voiceIdParam = (String) request.getOrDefault("voiceId",
                             request.getOrDefault("engineVoiceId", "M1"));
        String lang       = (String) request.getOrDefault("lang", "na");
        double speed      = request.get("speed")      instanceof Number n ? n.doubleValue() : 1.0;
        int totalSteps    = request.get("totalSteps") instanceof Number n ? n.intValue()    : 8;

        Number projectIdNum = (Number) request.get("projectId");
        Long projectId = projectIdNum != null ? projectIdNum.longValue() : null;

        // ── Validation ───────────────────────────────────────────────────────
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (text.length() > maxChars) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        Map<String, Object> usage = dashboardRepository.getUsageToday(userId);
        int charsUsed = ((Number) usage.get("characters_used")).intValue();
        if (charsUsed + text.length() > dailyLimit) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // ── Resolve voice ────────────────────────────────────────────────────
        List<Map<String, Object>> voices = jdbcTemplate.queryForList(
                "SELECT id FROM voice WHERE engine_voice_id = ?", voiceIdParam);
        Long voiceDbId = voices.isEmpty() ? 1L : ((Number) voices.get(0).get("id")).longValue();

        Long genId = dashboardRepository.createGeneration(userId, projectId, voiceDbId, text, text.length());

        try {
            // ── Call TTS service ─────────────────────────────────────────────
            byte[] audioBytes = supertonicClient.synthesize(text, voiceIdParam, lang, speed, totalSteps);

            // ── Save to disk ─────────────────────────────────────────────────
            File dir = new File(audioDir, userId.toString());
            if (!dir.exists()) dir.mkdirs();
            File audioFile = new File(dir, genId + ".wav");
            try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                fos.write(audioBytes);
            }
            dashboardRepository.updateGenerationSuccess(genId, audioFile.getAbsolutePath(), 0.0);
            dashboardRepository.upsertUsage(userId, text.length());

            // ── Return WAV bytes directly — zero JSON serialisation ──────────
            // Angular gets this as responseType:'blob', creates an Object URL and plays it.
            // The generation ID travels in X-Generation-Id header for future reference.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioBytes.length);
            headers.set("X-Generation-Id", String.valueOf(genId));
            headers.set("Access-Control-Expose-Headers", "X-Generation-Id");

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

        } catch (SupertonicClient.EngineUnreachableException e) {
            log.warn("TTS engine unreachable: {}", e.getMessage());
            dashboardRepository.updateGenerationFailed(genId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("TTS generation failed", e);
            dashboardRepository.updateGenerationFailed(genId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<?> getAudio(Authentication auth, @PathVariable Long id) {
        Long userId = getUserId(auth);
        Optional<String> path = dashboardRepository.getGenerationAudioPath(userId, id);
        if (path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
        return ResponseEntity.ok().build();
    }
}
