package com.voisetu.backend;

import lombok.Data;

@Data
public class TtsRequest {
    private String text;
    /** Voice preset ID: M1–M5 or F1–F5. Falls back to engineVoiceId if provided. */
    private String voiceId;
    /** Legacy field kept for backward compatibility — same semantics as voiceId */
    private String engineVoiceId;
    /** Language code: "hi", "en", "na" (Hinglish/auto). Defaults to "na". */
    private String lang;
    /** Speed multiplier (0.7 – 2.0). Defaults to 1.0. */
    private Double speed;
    /** Diffusion steps (1 – 40). Defaults to 8. */
    private Integer totalSteps;

    /** Returns the effective voice ID, preferring voiceId over engineVoiceId. */
    public String resolvedVoiceId() {
        if (voiceId != null && !voiceId.isBlank()) return voiceId;
        if (engineVoiceId != null && !engineVoiceId.isBlank()) return engineVoiceId;
        return "M1"; // safe default
    }

    public double resolvedSpeed()      { return speed      != null ? speed      : 1.0; }
    public int    resolvedTotalSteps() { return totalSteps != null ? totalSteps : 8;   }
    public String resolvedLang()       { return lang       != null ? lang       : "na"; }
}
