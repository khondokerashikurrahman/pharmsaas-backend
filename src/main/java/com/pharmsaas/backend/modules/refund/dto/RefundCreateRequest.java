package com.pharmsaas.backend.modules.refund.dto;

import java.math.BigDecimal;

public record RefundCreateRequest(
    String requestKey,
    BigDecimal amount,
    String paymentMethod,
    String transactionReference,
    String notes
) {}
