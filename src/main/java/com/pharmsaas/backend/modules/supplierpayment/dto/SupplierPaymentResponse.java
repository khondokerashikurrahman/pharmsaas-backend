package com.pharmsaas.backend.modules.supplierpayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SupplierPaymentResponse(
    Long id,
    Long tenantId,
    Long supplierId,
    String supplierCode,
    String supplierName,
    String paymentNumber,
    LocalDate paymentDate,
    BigDecimal amount,
    String paymentMethod,
    String referenceNumber,
    String notes,
    String status,
    Long createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
