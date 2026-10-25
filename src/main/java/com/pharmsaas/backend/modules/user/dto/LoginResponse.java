package com.pharmsaas.backend.modules.user.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        Long tenantId,
        String role,
        String tenantStatus,
        String membershipRole,
        List<TenantOptionResponse> availableTenants
) {
    public LoginResponse(
            String token,
            String tokenType,
            Long userId,
            String fullName,
            String email,
            Long tenantId,
            String role,
            String tenantStatus,
            String membershipRole
    ) {
        this(
                token,
                tokenType,
                userId,
                fullName,
                email,
                tenantId,
                role,
                tenantStatus,
                membershipRole,
                List.of()
        );
    }
}
