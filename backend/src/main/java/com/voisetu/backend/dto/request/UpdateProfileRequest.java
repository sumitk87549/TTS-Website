package com.voisetu.backend.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 40, message = "Display name must be between 2 and 40 characters")
        String displayName
) {}
