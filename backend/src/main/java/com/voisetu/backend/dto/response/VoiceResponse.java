package com.voisetu.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VoiceResponse(
        Long id,
        @JsonProperty("engineVoiceId")
        String engineVoiceId,
        @JsonProperty("displayName")
        String displayName,
        String gender,
        @JsonProperty("styleTag")
        String styleTag
) {
    @JsonProperty("engine_voice_id")
    public String engineVoiceIdSnake() {
        return engineVoiceId;
    }

    @JsonProperty("display_name")
    public String displayNameSnake() {
        return displayName;
    }

    @JsonProperty("style_tag")
    public String styleTagSnake() {
        return styleTag;
    }
}
