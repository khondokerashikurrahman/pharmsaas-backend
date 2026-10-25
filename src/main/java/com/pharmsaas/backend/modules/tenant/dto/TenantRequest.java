package com.pharmsaas.backend.modules.tenant.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequest(

        @NotBlank(message = "Tenant code is required")
        @Size(max = 30)
        String tenantCode,

        @NotBlank(message = "Tenant name is required")
        @Size(max = 150)
        String name,

        @Size(max = 150)
        String ownerName,

        @Email(message = "Invalid email address")
        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        String address,

        String status

) {}

