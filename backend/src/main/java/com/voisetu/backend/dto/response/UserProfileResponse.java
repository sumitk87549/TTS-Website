package com.voisetu.backend.dto.response;

import java.time.Instant;

public record UserProfileResponse(
        String email,
        String displayName,
        Instant createdAt,
        boolean isAdmin
) {}
