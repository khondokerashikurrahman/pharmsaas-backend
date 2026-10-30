package com.pharmsaas.backend.modules.salesman.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SalesmanResponse(
    Long id,
    Long tenantId,
    String employeeId,
    String name,
    String phone,
    String email,
    String address,
    String territory,
    BigDecimal commissionRate,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
