package com.pharmsaas.backend.modules.refund.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RefundResponse(
    Long id,
    Long saleReturnId,
    String refundNumber,
    String requestKey,
    BigDecimal amount,
    String paymentMethod,
    String transactionReference,
    String status,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime processedAt,
    Long createdById,
    Long processedById
) {}
