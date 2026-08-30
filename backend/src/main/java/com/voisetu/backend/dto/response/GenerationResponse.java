package com.voisetu.backend.dto.response;

import java.time.Instant;

public record GenerationResponse(
        Long id,
        String inputText,
        Double durationSeconds,
        String status,
        Instant createdAt,
        boolean isLiked,
        String voiceName
) {}
