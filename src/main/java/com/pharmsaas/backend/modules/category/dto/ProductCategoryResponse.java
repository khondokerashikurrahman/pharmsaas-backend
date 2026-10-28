package com.pharmsaas.backend.modules.category.dto;

import java.time.OffsetDateTime;

public record ProductCategoryResponse(
    Long id,
    Long tenantId,
    String productType,
    String name,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
