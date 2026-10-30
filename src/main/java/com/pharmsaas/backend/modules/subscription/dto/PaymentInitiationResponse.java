package com.pharmsaas.backend.modules.subscription.dto;

import java.math.BigDecimal;

public record PaymentInitiationResponse(
    String transactionReference,
    String gatewayUrl,
    BigDecimal amount,
    String currency,
    String status
) {}
