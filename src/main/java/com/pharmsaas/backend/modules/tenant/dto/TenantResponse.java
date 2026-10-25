package com.pharmsaas.backend.modules.tenant.dto;

import java.time.OffsetDateTime;


public record TenantResponse(
        Long id,
        String tenantCode,
        String name,
        String ownerName,
        String email,
        String phone,
        String address,
        String status,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

