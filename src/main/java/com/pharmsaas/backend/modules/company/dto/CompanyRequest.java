package com.pharmsaas.backend.modules.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
    @NotBlank(message = "Company name is required")
    @Size(max = 180)
    String name,

    @Size(max = 120)
    String contactPerson,

    @Size(max = 30)
    String phone,

    @Email(message = "Invalid email address")
    @Size(max = 180)
    String email,

    @Size(max = 500)
    String address,

    String status
) {}
