package com.voisetu.backend;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final DashboardRepository dashboardRepository;
    private final AppUserRepository userRepository;

    public UsageController(DashboardRepository dashboardRepository, AppUserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }
    
    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().id();
    }

    @GetMapping("/today")
    public Map<String, Object> getUsageToday(Authentication auth) {
        Map<String, Object> usage = dashboardRepository.getUsageToday(getUserId(auth));
        return Map.of(
            "charactersUsed", usage.get("characters_used"),
            "generationCount", usage.get("generation_count"),
            "charactersLimit", 5000 // In a real app this would read from properties
        );
    }
}
