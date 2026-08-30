package com.voisetu.backend.model;

import java.time.Instant;

public record AppUser(
    Long id,
    String email,
    String passwordHash,
    String displayName,
    boolean isAdmin,
    Instant createdAt
) {}
