package com.voisetu.backend;

import lombok.Data;

@Data
public class TtsRequest {
    private String text;
    private String engineVoiceId;
}
