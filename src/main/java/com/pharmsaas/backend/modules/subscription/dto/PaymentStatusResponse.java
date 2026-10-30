package com.pharmsaas.backend.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentStatusResponse(
    String transactionReference,
    String status,
    BigDecimal amount,
    String currency,
    String planName,
    OffsetDateTime verifiedAt,
    boolean registrationCompleted
) {}
