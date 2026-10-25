
package com.pharmsaas.backend.modules.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MedicineRequest(

        @NotBlank(message = "Medicine code is required")
        @Size(max = 50, message = "Medicine code cannot exceed 50 characters")
        String medicineCode,

        @NotBlank(message = "Medicine name is required")
        @Size(max = 200, message = "Medicine name cannot exceed 200 characters")
        String name,

        @Size(max = 200, message = "Generic name cannot exceed 200 characters")
        String genericName,

        @Size(max = 150, message = "Brand name cannot exceed 150 characters")
        String brandName,

        @Size(max = 50, message = "Strength cannot exceed 50 characters")
        String strength,

        @Size(max = 50, message = "Dosage form cannot exceed 50 characters")
        String dosageForm,

        @Size(max = 150, message = "Manufacturer cannot exceed 150 characters")
        String manufacturer,

        @Size(max = 100, message = "Barcode cannot exceed 100 characters")
        String barcode,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @Pattern(
                regexp = "(?i)^(Active|Inactive)?$",
                message = "Status must be Active or Inactive"
        )
        String status
) {}
