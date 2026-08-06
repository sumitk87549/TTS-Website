package com.voisetu.backend.service;

import com.voisetu.backend.DashboardRepository;
import com.voisetu.backend.SupertonicClient;
import com.voisetu.backend.dto.TtsGenerationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class TtsGenerationService {
    private final SupertonicClient supertonicClient;
    private final DashboardRepository dashboardRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AudioStorageService audioStorageService;
    private final int dailyLimit;
    private final int maxChars;

    public TtsGenerationService(SupertonicClient supertonicClient,
                                DashboardRepository dashboardRepository,
                                JdbcTemplate jdbcTemplate,
                                AudioStorageService audioStorageService,
                                @Value("${app.usage.daily-limit:5000}") int dailyLimit,
                                @Value("${app.usage.max-request-chars:1000}") int maxChars) {
        this.supertonicClient = supertonicClient;
        this.dashboardRepository = dashboardRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.audioStorageService = audioStorageService;
        this.dailyLimit = dailyLimit;
        this.maxChars = maxChars;
    }

    public GenerationResult generate(Long userId, TtsGenerationRequest request) throws Exception {
        String text = request.text();
        Long voiceDbId = resolveVoiceDbId(request.resolvedVoiceId());
        Long generationId = dashboardRepository.createGeneration(userId, request.projectId(), voiceDbId, text, text.length());

        try {
            byte[] audioBytes = supertonicClient.synthesize(text, request.resolvedVoiceId(), request.resolvedLang(),
                    request.resolvedSpeed(), request.resolvedTotalSteps());
            File audioFile = audioStorageService.saveWav(userId, generationId, audioBytes);
            dashboardRepository.updateGenerationSuccess(generationId, audioFile.getAbsolutePath(), 0.0);
            dashboardRepository.upsertUsage(userId, text.length());
            return new GenerationResult(generationId, audioBytes);
        } catch (Exception e) {
            dashboardRepository.updateGenerationFailed(generationId);
            throw e;
        }
    }

    public boolean isBlankText(TtsGenerationRequest request) {
        return request.text() == null || request.text().trim().isEmpty();
    }

    public boolean exceedsRequestLimit(TtsGenerationRequest request) {
        return request.text() != null && request.text().length() > maxChars;
    }

    public boolean exceedsDailyLimit(Long userId, TtsGenerationRequest request) {
        Map<String, Object> usage = dashboardRepository.getUsageToday(userId);
        int charsUsed = ((Number) usage.get("characters_used")).intValue();
        return charsUsed + request.text().length() > dailyLimit;
    }

    private Long resolveVoiceDbId(String engineVoiceId) {
        List<Map<String, Object>> voices = jdbcTemplate.queryForList("SELECT id FROM voice WHERE engine_voice_id = ?", engineVoiceId);
        return voices.isEmpty() ? 1L : ((Number) voices.get(0).get("id")).longValue();
    }

    public record GenerationResult(Long generationId, byte[] audioBytes) { }
}
