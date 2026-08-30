package com.voisetu.backend.dto.response;

public record VoiceResponse(
        Long id,
        String engineVoiceId,
        String displayName,
        String gender,
        String styleTag
) {}
