package com.pharmsaas.backend.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SelectTenantRequest(
        @NotNull(message = "Tenant ID is required")
        @Positive(message = "Tenant ID must be positive")
        Long tenantId
) {
}
