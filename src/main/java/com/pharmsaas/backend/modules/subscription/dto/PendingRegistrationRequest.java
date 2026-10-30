package com.pharmsaas.backend.modules.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PendingRegistrationRequest(
        @NotBlank @Size(max = 150) String pharmacyName,
        @NotBlank @Size(max = 150) String ownerName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 5000) String address,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 30) String planCode
) {}
