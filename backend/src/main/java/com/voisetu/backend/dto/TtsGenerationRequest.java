package com.voisetu.backend.dto;

public record TtsGenerationRequest(
        String text,
        String voiceId,
        String engineVoiceId,
        String lang,
        Double speed,
        Integer totalSteps,
        Long projectId
) {
    public String resolvedVoiceId() {
        if (voiceId != null && !voiceId.isBlank()) return voiceId;
        if (engineVoiceId != null && !engineVoiceId.isBlank()) return engineVoiceId;
        return "M1";
    }

    public String resolvedLang() { return lang == null || lang.isBlank() ? "na" : lang; }
    public double resolvedSpeed() { return speed == null ? 1.0 : speed; }
    public int resolvedTotalSteps() { return totalSteps == null ? 8 : totalSteps; }
}
