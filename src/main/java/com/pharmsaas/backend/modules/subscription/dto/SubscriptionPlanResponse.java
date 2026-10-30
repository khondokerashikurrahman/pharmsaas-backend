package com.pharmsaas.backend.modules.subscription.dto;

import java.math.BigDecimal;

public record SubscriptionPlanResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer durationDays
) {
}
