package com.voisetu.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
        String name
) {}
