package com.voisetu.backend.controller;

import com.voisetu.backend.repository.DashboardRepository;

import com.voisetu.backend.config.AppProperties;
import com.voisetu.backend.dto.response.UsageResponse;
import com.voisetu.backend.service.AuthenticatedUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Usage quota endpoint.
 * GET /api/usage/today — returns current-day characters used / limit.
 */
@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final DashboardRepository dashboardRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AppProperties appProperties;

    public UsageController(DashboardRepository dashboardRepository,
                           AuthenticatedUserService authenticatedUserService,
                           AppProperties appProperties) {
        this.dashboardRepository = dashboardRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.appProperties = appProperties;
    }

    @GetMapping("/today")
    public UsageResponse getUsageToday(Authentication auth) {
        Map<String, Object> usage = dashboardRepository.getUsageToday(
                authenticatedUserService.userId(auth));

        return new UsageResponse(
                ((Number) usage.get("characters_used")).intValue(),
                ((Number) usage.get("generation_count")).intValue(),
                appProperties.getUsage().getDailyLimit()  // reads from config, not hardcoded
        );
    }
}
