package com.voisetu.backend.controller;

import com.voisetu.backend.repository.DashboardRepository;

import com.voisetu.backend.dto.request.TtsGenerateRequest;
import com.voisetu.backend.exception.ResourceNotFoundException;
import com.voisetu.backend.service.AuthenticatedUserService;
import com.voisetu.backend.service.TtsGenerationService;
import jakarta.validation.Valid;
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

/**
 * Authenticated TTS generation endpoints.
 *
 * POST /api/tts/generate    — generate speech; returns WAV bytes
 * GET  /api/generations/{id}/audio — stream saved WAV file
 * POST /api/generations/{id}/like  — toggle like on a generation
 */
@RestController
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

    @PostMapping("/api/tts/generate")
    public ResponseEntity<byte[]> generate(Authentication auth,
                                           @Valid @RequestBody TtsGenerateRequest request) throws Exception {
        Long userId = authenticatedUserService.userId(auth);
        // Business rule validation is handled inside TtsGenerationService (throws typed exceptions)
        // Exceptions propagate to GlobalExceptionHandler → structured ApiError JSON
        TtsGenerationService.GenerationResult result = ttsGenerationService.generate(userId, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.setContentLength(result.audioBytes().length);
        headers.set("X-Generation-Id", String.valueOf(result.generationId()));
        headers.set("Access-Control-Expose-Headers", "X-Generation-Id");

        return new ResponseEntity<>(result.audioBytes(), headers, HttpStatus.OK);
    }

    /**
     * Stream the saved WAV file for a specific generation.
     * Path: /api/generations/{id}/audio
     */
    @GetMapping("/api/generations/{id}/audio")
    public ResponseEntity<FileSystemResource> getAudio(Authentication auth, @PathVariable Long id) {
        Long userId = authenticatedUserService.userId(auth);
        Optional<String> path = dashboardRepository.getGenerationAudioPath(userId, id);

        if (path.isEmpty()) {
            throw new ResourceNotFoundException("Generation audio", id);
        }
        File file = new File(path.get());
        if (!file.exists()) {
            throw new ResourceNotFoundException("Audio file", id);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.setContentLength(file.length());
        return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    }

    /**
     * Toggle like/unlike on a generation.
     * Path: /api/generations/{id}/like
     */
    @PostMapping("/api/generations/{id}/like")
    public ResponseEntity<Void> toggleLike(Authentication auth, @PathVariable Long id) {
        Long userId = authenticatedUserService.userId(auth);
        dashboardRepository.toggleGenerationLike(userId, id);
        return ResponseEntity.ok().build();
    }
}
