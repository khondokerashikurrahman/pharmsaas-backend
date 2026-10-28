package com.pharmsaas.backend.modules.generic.dto;

import java.time.OffsetDateTime;

public record ProductGenericResponse(
    Long id,
    Long tenantId,
    String name,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
