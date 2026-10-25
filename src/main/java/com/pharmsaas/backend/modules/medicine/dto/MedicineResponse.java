package com.pharmsaas.backend.modules.medicine.dto;

import java.time.OffsetDateTime;

public record MedicineResponse(
        Long id,
        String medicineCode,
        String name,
        String genericName,
        String brandName,
        String strength,
        String dosageForm,
        String manufacturer,
        String barcode,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

