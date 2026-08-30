package com.voisetu.backend.dto.request;

import jakarta.validation.constraints.*;

/** Request DTO for unauthenticated voice preview (POST /api/public/tts/preview). */
public record TtsPreviewRequest(

        @NotBlank(message = "Preview text is required")
        @Size(max = 300, message = "Preview text must not exceed 300 characters")
        String text,

        @NotBlank(message = "Voice ID is required")
        @Pattern(regexp = "[MF][1-5]", message = "Voice ID must be one of: M1–M5 or F1–F5")
        String voiceId,

        @Pattern(regexp = "hi|en|na", message = "Language must be 'hi', 'en', or 'na'")
        String lang
) {
    public String resolvedLang() { return lang == null || lang.isBlank() ? "na" : lang; }
}
