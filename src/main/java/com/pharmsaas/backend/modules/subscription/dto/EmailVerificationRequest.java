package com.pharmsaas.backend.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must contain exactly 6 digits.")
        String otp
) {}
