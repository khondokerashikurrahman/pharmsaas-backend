package com.pharmsaas.backend.modules.supplier.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SupplierResponse(
    Long id,
    Long tenantId,
    String supplierCode,
    String name,
    String companyName,
    String contactPerson,
    String phone,
    String email,
    String address,
    BigDecimal openingBalance,
    BigDecimal creditLimit,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
