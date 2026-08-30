package com.voisetu.backend.dto.response;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        Instant createdAt
) {}
