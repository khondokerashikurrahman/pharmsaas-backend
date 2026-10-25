package com.pharmsaas.backend.modules.tenant.dto;


public record PublicTenantRegistrationResponse(
        Long tenantId,
        String tenantCode,
        Long ownerUserId,
        String pharmacyName,
        String email,
        String status,
        String message
) {}
