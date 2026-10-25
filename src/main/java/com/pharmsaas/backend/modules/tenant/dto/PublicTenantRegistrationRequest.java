package com.pharmsaas.backend.modules.tenant.dto;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicTenantRegistrationRequest(

        @NotBlank(message = "Pharmacy name is required")
        @Size(max = 150)
        String pharmacyName,

        @NotBlank(message = "Owner name is required")
        @Size(max = 150)
        String ownerName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 5000)
        String address,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72,
                message = "Password must be between 8 and 72 characters")
        String password

) {}
