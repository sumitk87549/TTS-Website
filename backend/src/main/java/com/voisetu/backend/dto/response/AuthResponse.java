package com.voisetu.backend.dto.response;

public record AuthResponse(
        String token,
        Long userId,
        String displayName,
        boolean isAdmin
) {}
