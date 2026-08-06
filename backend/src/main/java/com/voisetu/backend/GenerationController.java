package com.voisetu.backend;

import com.voisetu.backend.dto.TtsGenerationRequest;
import com.voisetu.backend.service.AuthenticatedUserService;
import com.voisetu.backend.service.TtsGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Optional;

@RestController
@RequestMapping("/api/tts")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);
    private final TtsGenerationService ttsGenerationService;
    private final DashboardRepository dashboardRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public GenerationController(TtsGenerationService ttsGenerationService,
                                DashboardRepository dashboardRepository,
                                AuthenticatedUserService authenticatedUserService) {
        this.ttsGenerationService = ttsGenerationService;
        this.dashboardRepository = dashboardRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(Authentication auth, @RequestBody TtsGenerationRequest request) {
        Long userId = authenticatedUserService.userId(auth);

        if (ttsGenerationService.isBlankText(request)) return ResponseEntity.badRequest().build();
        if (ttsGenerationService.exceedsRequestLimit(request)) return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        if (ttsGenerationService.exceedsDailyLimit(userId, request)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();

        try {
            TtsGenerationService.GenerationResult result = ttsGenerationService.generate(userId, request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(result.audioBytes().length);
            headers.set("X-Generation-Id", String.valueOf(result.generationId()));
            headers.set("Access-Control-Expose-Headers", "X-Generation-Id");
            return new ResponseEntity<>(result.audioBytes(), headers, HttpStatus.OK);
        } catch (SupertonicClient.EngineUnreachableException e) {
            log.warn("TTS engine unreachable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("TTS generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<?> getAudio(Authentication auth, @PathVariable Long id) {
        Long userId = authenticatedUserService.userId(auth);
        Optional<String> path = dashboardRepository.getGenerationAudioPath(userId, id);
        if (path.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        File file = new File(path.get());
        if (!file.exists()) return ResponseEntity.notFound().build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.setContentLength(file.length());
        return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(Authentication auth, @PathVariable Long id) {
        Long userId = authenticatedUserService.userId(auth);
        dashboardRepository.toggleGenerationLike(userId, id);
        return ResponseEntity.ok().build();
    }
}
