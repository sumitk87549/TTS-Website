package com.voisetu.backend;

import com.voisetu.backend.dto.request.ProjectRequest;
import com.voisetu.backend.dto.response.ProjectResponse;
import com.voisetu.backend.service.AuthenticatedUserService;
import com.voisetu.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuthenticatedUserService authenticatedUserService;

    public ProjectController(ProjectService projectService,
                             AuthenticatedUserService authenticatedUserService) {
        this.projectService = projectService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public List<ProjectResponse> getProjects(Authentication auth) {
        return projectService.listForUser(authenticatedUserService.userId(auth));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(Authentication auth,
                                                          @Valid @RequestBody ProjectRequest request) {
        ProjectResponse created = projectService.create(authenticatedUserService.userId(auth), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateProject(Authentication auth,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ProjectRequest request) {
        projectService.rename(authenticatedUserService.userId(auth), id, request.name());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(Authentication auth, @PathVariable Long id) {
        projectService.delete(authenticatedUserService.userId(auth), id);
        return ResponseEntity.noContent().build();
    }
}
