package com.voisetu.backend;

import com.voisetu.backend.dto.ProjectRequest;
import com.voisetu.backend.service.AuthenticatedUserService;
import com.voisetu.backend.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final AuthenticatedUserService authenticatedUserService;

    public ProjectController(ProjectService projectService, AuthenticatedUserService authenticatedUserService) {
        this.projectService = projectService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public List<Map<String, Object>> getProjects(Authentication auth) {
        return projectService.listForUser(authenticatedUserService.userId(auth));
    }

    @PostMapping
    public ResponseEntity<?> createProject(Authentication auth, @RequestBody ProjectRequest request) {
        Long id = projectService.create(authenticatedUserService.userId(auth), request.name());
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProject(Authentication auth, @PathVariable Long id, @RequestBody ProjectRequest request) {
        projectService.rename(authenticatedUserService.userId(auth), id, request.name());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(Authentication auth, @PathVariable Long id) {
        projectService.delete(authenticatedUserService.userId(auth), id);
        return ResponseEntity.ok().build();
    }
}
