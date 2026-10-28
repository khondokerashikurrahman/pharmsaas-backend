package com.pharmsaas.backend.modules.dosageform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DosageFormRequest(
    @NotBlank(message = "Dosage form name is required")
    @Size(max = 100, message = "Dosage form name cannot exceed 100 characters")
    String name
) {}
