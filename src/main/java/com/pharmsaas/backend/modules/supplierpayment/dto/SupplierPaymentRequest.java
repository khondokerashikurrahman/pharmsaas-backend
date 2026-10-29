package com.pharmsaas.backend.modules.supplierpayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplierPaymentRequest(
    Long supplierId,
    LocalDate paymentDate,
    BigDecimal amount,
    String paymentMethod,
    String referenceNumber,
    String notes
) {}
