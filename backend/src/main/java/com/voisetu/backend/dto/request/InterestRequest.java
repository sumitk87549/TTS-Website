package com.voisetu.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InterestRequest(
        @NotBlank(message = "wouldPay is required")
        @Pattern(regexp = "yes|no|maybe", message = "wouldPay must be 'yes', 'no', or 'maybe'")
        String wouldPay,

        Integer suggestedPriceInr,

        String comment
) {}
