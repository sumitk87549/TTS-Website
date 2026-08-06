package com.voisetu.backend.service;

import com.voisetu.backend.DashboardRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProjectService {
    private final DashboardRepository dashboardRepository;

    public ProjectService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public List<Map<String, Object>> listForUser(Long userId) {
        return dashboardRepository.getProjects(userId);
    }

    public Long create(Long userId, String name) {
        return dashboardRepository.createProject(userId, name);
    }

    public void rename(Long userId, Long projectId, String name) {
        dashboardRepository.updateProject(userId, projectId, name);
    }

    public void delete(Long userId, Long projectId) {
        dashboardRepository.deleteProject(userId, projectId);
    }
}
