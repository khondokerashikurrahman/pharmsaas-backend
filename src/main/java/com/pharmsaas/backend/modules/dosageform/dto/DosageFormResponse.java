package com.pharmsaas.backend.modules.dosageform.dto;

import java.time.OffsetDateTime;

public record DosageFormResponse(
    Long id,
    Long tenantId,
    String name,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
