package com.voisetu.backend.service;

import com.voisetu.backend.repository.DashboardRepository;
import com.voisetu.backend.dto.request.ProjectRequest;
import com.voisetu.backend.dto.response.ProjectResponse;
import com.voisetu.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for project CRUD operations.
 * Returns typed {@link ProjectResponse} DTOs instead of raw {@code Map<String,Object>}.
 */
@Service
public class ProjectService {

    private final DashboardRepository dashboardRepository;

    public ProjectService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public List<ProjectResponse> listForUser(Long userId) {
        return dashboardRepository.getProjects(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse create(Long userId, String name) {
        Long id = dashboardRepository.createProject(userId, name);
        return new ProjectResponse(id, name, Instant.now());
    }

    @Transactional
    public void rename(Long userId, Long projectId, String name) {
        dashboardRepository.updateProject(userId, projectId, name);
    }

    @Transactional
    public void delete(Long userId, Long projectId) {
        dashboardRepository.deleteProject(userId, projectId);
    }

    private ProjectResponse toResponse(Map<String, Object> row) {
        return new ProjectResponse(
                ((Number) row.get("id")).longValue(),
                (String) row.get("name"),
                row.get("created_at") instanceof java.sql.Timestamp ts ? ts.toInstant() : Instant.now()
        );
    }
}
