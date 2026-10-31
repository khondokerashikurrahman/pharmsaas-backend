package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.dto.PendingRegistrationRequest;
import com.pharmsaas.backend.modules.subscription.dto.PendingRegistrationResponse;
import com.pharmsaas.backend.modules.subscription.entity.PendingRegistration;
import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPlan;
import com.pharmsaas.backend.modules.subscription.repository.PendingRegistrationRepository;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPlanRepository;
import com.pharmsaas.backend.modules.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class PendingRegistrationService {

    private final PendingRegistrationRepository pendingRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PendingRegistrationService(
            PendingRegistrationRepository pendingRepository,
            SubscriptionPlanRepository planRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.pendingRepository = pendingRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PendingRegistrationResponse create(PendingRegistrationRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        OffsetDateTime now = OffsetDateTime.now();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        PendingRegistration existingPending = pendingRepository
                .findFirstByEmailAndStatusAndExpiresAtAfter(
                        email, "PENDING_EMAIL_VERIFICATION", now)
                .orElse(null);

        if (existingPending != null) {
            SubscriptionPlan existingPlan = existingPending.getPlan();
            return new PendingRegistrationResponse(
                    existingPending.getRegistrationReference(),
                    existingPending.getPharmacyName(),
                    existingPlan.getCode(),
                    existingPlan.getName(),
                    existingPlan.getPrice(),
                    existingPlan.getCurrency(),
                    existingPlan.getDurationDays(),
                    existingPending.getStatus(),
                    existingPending.getExpiresAt(),
                    "An existing registration was found. Continue email verification using the existing registration."
            );
        }

        if (pendingRepository.existsByEmailAndStatusAndExpiresAtAfter(
                email, "PENDING_PAYMENT", now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This email already has a verified registration awaiting payment. Please continue that registration."
            );
        }

        String planCode = request.planCode().trim().toUpperCase(Locale.ROOT);

        SubscriptionPlan plan = planRepository.findByCodeAndActiveTrue(planCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selected subscription plan is unavailable."
                ));

        PendingRegistration pending = new PendingRegistration();
        pending.setRegistrationReference(generateReference());
        pending.setPharmacyName(request.pharmacyName().trim());
        pending.setOwnerName(request.ownerName().trim());
        pending.setEmail(email);
        pending.setPhone(request.phone() == null ? null : request.phone().trim());
        pending.setAddress(request.address() == null ? null : request.address().trim());
        pending.setPasswordHash(passwordEncoder.encode(request.password()));
        pending.setPlan(plan);
        pending.setStatus("PENDING_EMAIL_VERIFICATION");
        pending.setExpiresAt(now.plusMinutes(30));

        PendingRegistration saved = pendingRepository.save(pending);

        return new PendingRegistrationResponse(
                saved.getRegistrationReference(),
                saved.getPharmacyName(),
                plan.getCode(),
                plan.getName(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getDurationDays(),
                saved.getStatus(),
                saved.getExpiresAt(),
                "Registration created. Verify your email before proceeding to payment."
        );
    }

    private String generateReference() {
        String reference;
        do {
            reference = "REG-" + UUID.randomUUID()
                    .toString().replace("-", "")
                    .substring(0, 20).toUpperCase(Locale.ROOT);
        } while (pendingRepository.existsByRegistrationReference(reference));

        return reference;
    }
}
