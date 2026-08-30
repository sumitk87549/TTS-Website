package com.voisetu.backend.controller;

import com.voisetu.backend.repository.DashboardRepository;

import com.voisetu.backend.dto.response.GenerationResponse;
import com.voisetu.backend.service.AuthenticatedUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generation history endpoints.
 *
 * GET    /api/generations          — paginated list with optional project filter
 * DELETE /api/generations/{id}     — delete a generation and its audio file
 */
@RestController
@RequestMapping("/api/generations")
public class HistoryController {

    private final DashboardRepository dashboardRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public HistoryController(DashboardRepository dashboardRepository,
                             AuthenticatedUserService authenticatedUserService) {
        this.dashboardRepository = dashboardRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public List<GenerationResponse> getGenerations(
            Authentication auth,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Guard against absurd page sizes
        int effectiveSize = Math.min(size, 100);
        int offset = page * effectiveSize;

        return dashboardRepository
                .getGenerations(authenticatedUserService.userId(auth), projectId, effectiveSize, offset)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGeneration(Authentication auth, @PathVariable Long id) {
        Long userId = authenticatedUserService.userId(auth);
        Optional<String> path = dashboardRepository.getGenerationAudioPath(userId, id);
        path.ifPresent(p -> new File(p).delete());
        dashboardRepository.deleteGeneration(userId, id);
        return ResponseEntity.noContent().build();
    }

    private GenerationResponse toResponse(Map<String, Object> row) {
        Object createdAtObj = row.get("created_at");
        Instant createdAt = createdAtObj instanceof java.sql.Timestamp ts
                ? ts.toInstant() : Instant.now();

        Object durationObj = row.get("duration_seconds");
        Double duration = durationObj instanceof Number n ? n.doubleValue() : null;

        boolean isLiked = row.get("is_liked") instanceof Boolean b && b;

        return new GenerationResponse(
                ((Number) row.get("id")).longValue(),
                (String) row.get("input_text"),
                duration,
                (String) row.get("status"),
                createdAt,
                isLiked,
                (String) row.get("voice_name")
        );
    }
}
