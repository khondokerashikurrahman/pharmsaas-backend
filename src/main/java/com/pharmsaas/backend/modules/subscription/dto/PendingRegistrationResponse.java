package com.pharmsaas.backend.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PendingRegistrationResponse(
        String registrationReference,
        String pharmacyName,
        String planCode,
        String planName,
        BigDecimal amount,
        String currency,
        Integer durationDays,
        String status,
        OffsetDateTime expiresAt,
        String message
) {}
