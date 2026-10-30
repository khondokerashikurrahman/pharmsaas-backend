package com.pharmsaas.backend.modules.subscription.controller;

import com.pharmsaas.backend.modules.subscription.dto.EmailVerificationRequest;
import com.pharmsaas.backend.modules.subscription.dto.EmailVerificationResponse;
import com.pharmsaas.backend.modules.subscription.dto.PendingRegistrationRequest;
import com.pharmsaas.backend.modules.subscription.dto.PendingRegistrationResponse;
import com.pharmsaas.backend.modules.subscription.service.PendingRegistrationService;
import com.pharmsaas.backend.modules.subscription.service.RegistrationEmailVerificationService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/subscription/registration")
public class PublicPendingRegistrationController {

    private final PendingRegistrationService registrationService;
    private final RegistrationEmailVerificationService verificationService;

    public PublicPendingRegistrationController(
            PendingRegistrationService registrationService,
            RegistrationEmailVerificationService verificationService) {
        this.registrationService = registrationService;
        this.verificationService = verificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PendingRegistrationResponse register(
            @Valid @RequestBody PendingRegistrationRequest request) {
        return registrationService.create(request);
    }

    @PostMapping("/{reference}/send-verification")
    public EmailVerificationResponse sendVerification(
            @PathVariable String reference) {
        return verificationService.sendVerification(reference);
    }

    @PostMapping("/{reference}/verify-email")
    public EmailVerificationResponse verifyEmail(
            @PathVariable String reference,
            @Valid @RequestBody EmailVerificationRequest request) {
        return verificationService.verifyEmail(reference, request.otp());
    }
}
