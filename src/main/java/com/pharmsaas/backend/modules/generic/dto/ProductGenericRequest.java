package com.pharmsaas.backend.modules.generic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductGenericRequest(
    @NotBlank(message = "Generic name is required")
    @Size(max = 150, message = "Generic name cannot exceed 150 characters")
    String name
) {}
