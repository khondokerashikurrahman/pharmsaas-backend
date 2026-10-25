package com.pharmsaas.backend.modules.user.dto;

public record TenantOptionResponse(
        Long tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String membershipRole
) {
}
