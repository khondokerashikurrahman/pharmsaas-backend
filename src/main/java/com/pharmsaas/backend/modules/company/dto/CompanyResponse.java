package com.pharmsaas.backend.modules.company.dto;

import java.time.OffsetDateTime;

public record CompanyResponse(
    Long id,
    Long tenantId,
    String companyCode,
    String name,
    String contactPerson,
    String phone,
    String email,
    String address,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
