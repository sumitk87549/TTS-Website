package com.voisetu.backend.dto.response;

public record UsageResponse(
        int charactersUsed,
        int generationCount,
        int charactersLimit
) {}
