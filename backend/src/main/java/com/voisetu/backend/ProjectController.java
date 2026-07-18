package com.voisetu.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final DashboardRepository dashboardRepository;
    private final AppUserRepository userRepository;

    public ProjectController(DashboardRepository dashboardRepository, AppUserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().id();
    }

    @GetMapping
    public List<Map<String, Object>> getProjects(Authentication auth) {
        return dashboardRepository.getProjects(getUserId(auth));
    }

    @PostMapping
    public ResponseEntity<?> createProject(Authentication auth, @RequestBody Map<String, String> body) {
        Long id = dashboardRepository.createProject(getUserId(auth), body.get("name"));
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProject(Authentication auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        dashboardRepository.updateProject(getUserId(auth), id, body.get("name"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(Authentication auth, @PathVariable Long id) {
        dashboardRepository.deleteProject(getUserId(auth), id);
        return ResponseEntity.ok().build();
    }
}
