package com.pharmsaas.backend.modules.subscription.dto;

public record EmailVerificationResponse(
        boolean success,
        boolean emailVerified,
        String status,
        String message
) {}
