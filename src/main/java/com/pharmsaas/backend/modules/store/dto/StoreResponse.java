
        package com.pharmsaas.backend.modules.store.dto;

import java.time.OffsetDateTime;

public record StoreResponse(
        Long id,
        Long tenantId,
        String storeCode,
        String name,
        String phone,
        String email,
        String address,
        String city,
        String district,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
