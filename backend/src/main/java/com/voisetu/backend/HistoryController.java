package com.voisetu.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/generations")
public class HistoryController {

    private final DashboardRepository dashboardRepository;
    private final AppUserRepository userRepository;

    public HistoryController(DashboardRepository dashboardRepository, AppUserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }
    
    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().id();
    }

    @GetMapping
    public List<Map<String, Object>> getGenerations(
            Authentication auth, 
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int offset = page * size;
        return dashboardRepository.getGenerations(getUserId(auth), projectId, size, offset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGeneration(Authentication auth, @PathVariable Long id) {
        Long userId = getUserId(auth);
        Optional<String> path = dashboardRepository.getGenerationAudioPath(userId, id);
        if (path.isPresent() && path.get() != null) {
            new File(path.get()).delete();
        }
        dashboardRepository.deleteGeneration(userId, id);
        return ResponseEntity.ok().build();
    }
}
